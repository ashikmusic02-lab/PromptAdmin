package com.example.util

import java.net.URI
import java.util.Locale
import java.util.UUID

sealed class UrlValidationResult {
    data object Empty : UrlValidationResult()
    data class Valid(
        val normalizedUrl: String,
        val detectedFormat: String,
        val host: String
    ) : UrlValidationResult()
    data class Invalid(val reason: String) : UrlValidationResult()
}

object UrlAndDeepLinkValidator {

    private val DRIVE_FILE_D_REGEX = Regex("""drive\.google\.com/file/d/([a-zA-Z0-9_-]+)""")
    private val DRIVE_ID_QUERY_REGEX = Regex("""(?:drive|docs)\.google\.com/(?:open|uc|thumbnail)\?(?:[^#]*&)?id=([a-zA-Z0-9_-]+)""")

    /**
     * Normalizes any public image URL.
     * If the URL is a Google Drive sharing/view link:
     * - drive.google.com/file/d/FILE_ID/view...
     * - drive.google.com/open?id=FILE_ID
     * - drive.google.com/uc?id=FILE_ID
     * converts it to: https://drive.google.com/uc?export=view&id=FILE_ID
     */
    fun normalizePublicImageUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return ""

        DRIVE_FILE_D_REGEX.find(trimmed)?.groupValues?.getOrNull(1)?.let { fileId ->
            if (fileId.isNotBlank()) {
                return "https://drive.google.com/uc?export=view&id=$fileId"
            }
        }

        DRIVE_ID_QUERY_REGEX.find(trimmed)?.groupValues?.getOrNull(1)?.let { fileId ->
            if (fileId.isNotBlank()) {
                return "https://drive.google.com/uc?export=view&id=$fileId"
            }
        }

        return trimmed
    }

    /**
     * Validates any public HTTP/HTTPS image URL without restricting by file extension.
     */
    fun validatePublicImageUrl(rawUrl: String): UrlValidationResult {
        val normalized = normalizePublicImageUrl(rawUrl)
        if (normalized.isEmpty()) return UrlValidationResult.Empty

        if (!normalized.startsWith("https://", ignoreCase = true) &&
            !normalized.startsWith("http://", ignoreCase = true)
        ) {
            return UrlValidationResult.Invalid("লিংকটি https:// অথবা http:// দিয়ে শুরু হতে হবে")
        }

        if (normalized.contains(" ")) {
            return UrlValidationResult.Invalid("লিংকের ভেতরে কোনো ফাঁকা স্থান (space) থাকা যাবে না")
        }

        val uri = try {
            URI(normalized)
        } catch (_: Exception) {
            return UrlValidationResult.Invalid("সঠিক ছবির লিংক দিন")
        }

        val host = uri.host?.lowercase(Locale.US)
        if (host.isNullOrBlank() || !host.contains(".")) {
            return UrlValidationResult.Invalid("সঠিক ছবির লিংক দিন")
        }

        return UrlValidationResult.Valid(
            normalizedUrl = normalized,
            detectedFormat = "IMAGE",
            host = host
        )
    }

    /**
     * Legacy method retained for unit test compatibility.
     */
    fun validateDirectImageUrl(rawUrl: String): UrlValidationResult {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return UrlValidationResult.Empty
        if (!trimmed.startsWith("https://", ignoreCase = true) &&
            !trimmed.startsWith("http://", ignoreCase = true)
        ) {
            return UrlValidationResult.Invalid("Invalid URL scheme")
        }
        val uri = try {
            URI(trimmed)
        } catch (_: Exception) {
            return UrlValidationResult.Invalid("Malformed URL")
        }
        val host = uri.host?.lowercase(Locale.US) ?: return UrlValidationResult.Invalid("Missing host")
        if (host.contains("firebasestorage.googleapis.com")) {
            return UrlValidationResult.Invalid("Firebase Storage URL not allowed")
        }
        val pathLower = (uri.path ?: "").lowercase(Locale.US)
        if (pathLower.endsWith(".html") || pathLower.endsWith(".htm")) {
            return UrlValidationResult.Invalid("HTML page is not an image")
        }
        val ext = when {
            pathLower.endsWith(".jpg") -> "JPG"
            pathLower.endsWith(".jpeg") -> "JPEG"
            pathLower.endsWith(".png") -> "PNG"
            pathLower.endsWith(".webp") -> "WEBP"
            else -> "IMAGE"
        }
        return UrlValidationResult.Valid(
            normalizedUrl = normalizePublicImageUrl(trimmed),
            detectedFormat = ext,
            host = host
        )
    }

    /**
     * Auto-generates shortName from title (max 80 chars).
     */
    fun generateShortNameFromTitle(title: String): String {
        val cleaned = title.trim()
            .replace(Regex("\\s+"), " ")
            .take(80)
        return cleaned.ifEmpty { "image" }
    }

    /**
     * Generates lowercase title words joined by comma (max 500 chars).
     */
    fun generateKeywordsFromTitle(title: String): String {
        val words = title.trim()
            .lowercase(Locale.ROOT)
            .split(Regex("[\\s,।;:|/\\\\]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        return words.joinToString(",").take(500)
    }

    fun generateUniqueImageId(shortName: String): String {
        val cleanSlug = shortName
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(18)
            .ifEmpty { "image" }
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
        return "img_${cleanSlug}_$suffix"
    }

    /**
     * Builds the internal application deep link (`promptlens://image/{imageId}`).
     */
    fun buildAutoDeepLink(imageId: String, scheme: String = "promptlens"): String {
        val safeScheme = scheme.trim().lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "").ifEmpty { "promptlens" }
        return "$safeScheme://image/$imageId"
    }
}
