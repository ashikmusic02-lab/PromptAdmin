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

    private val SUPPORTED_IMAGE_EXTENSIONS = listOf(".jpg", ".jpeg", ".png", ".webp")
    private val KNOWN_DIRECT_IMAGE_HOSTS = listOf(
        "images.unsplash.com",
        "plus.unsplash.com",
        "images.pexels.com",
        "cdn.pixabay.com",
        "i.imgur.com",
        "lh3.googleusercontent.com",
        "res.cloudinary.com"
    )

    /**
     * Validates a direct external image URL.
     * Enforces HTTPS/HTTP protocol, valid hostname, and supported direct image format
     * (.jpg, .jpeg, .png, .webp) or recognized direct image CDN parameter.
     */
    fun validateDirectImageUrl(rawUrl: String): UrlValidationResult {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return UrlValidationResult.Empty

        if (!trimmed.startsWith("https://", ignoreCase = true) &&
            !trimmed.startsWith("http://", ignoreCase = true)
        ) {
            return UrlValidationResult.Invalid("URL must start with https:// or http://")
        }

        if (trimmed.contains(" ")) {
            return UrlValidationResult.Invalid("URL must not contain spaces.")
        }

        val uri = try {
            URI(trimmed)
        } catch (e: Exception) {
            return UrlValidationResult.Invalid("Malformed URL syntax: ${e.localizedMessage ?: "Invalid URI"}")
        }

        val host = uri.host?.lowercase(Locale.US)
        if (host.isNullOrBlank() || !host.contains(".")) {
            return UrlValidationResult.Invalid("URL must include a valid domain host.")
        }

        if (host.contains("firebasestorage.googleapis.com")) {
            return UrlValidationResult.Invalid("Firebase Storage uploads are prohibited by policy. Use an external direct image URL.")
        }

        val pathLower = (uri.path ?: "").lowercase(Locale.US)
        val queryLower = (uri.query ?: "").lowercase(Locale.US)

        val matchedExt = SUPPORTED_IMAGE_EXTENSIONS.firstOrNull { ext ->
            pathLower.endsWith(ext) || queryLower.contains("fm=${ext.removePrefix(".")}") ||
                queryLower.contains("format=${ext.removePrefix(".")}")
        }

        val isKnownImageCdn = KNOWN_DIRECT_IMAGE_HOSTS.any { host == it || host.endsWith(".$it") }

        return when {
            matchedExt != null -> {
                UrlValidationResult.Valid(
                    normalizedUrl = trimmed,
                    detectedFormat = matchedExt.removePrefix(".").uppercase(Locale.US),
                    host = host
                )
            }
            isKnownImageCdn && pathLower.length > 1 -> {
                val formatGuess = when {
                    queryLower.contains("webp") -> "WEBP"
                    queryLower.contains("png") -> "PNG"
                    else -> "JPG/CDN"
                }
                UrlValidationResult.Valid(
                    normalizedUrl = trimmed,
                    detectedFormat = formatGuess,
                    host = host
                )
            }
            else -> {
                UrlValidationResult.Invalid(
                    "Direct image link required (.jpg, .jpeg, .png, or .webp). Supplied path does not end with a supported image extension."
                )
            }
        }
    }

    /**
     * Generates a deterministic, URL-safe Firestore document ID for a newly created image
     * so the administrator never needs to invent an ID manually.
     */
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
     * Automatically builds the internal application deep link (`scheme://image/IMAGE_ID`)
     * and verified Android App Link (`https://host/image/IMAGE_ID`).
     */
    fun buildAutoDeepLink(scheme: String, imageId: String): String {
        val safeScheme = scheme.trim().lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "").ifEmpty { "promptvault" }
        return "$safeScheme://image/$imageId"
    }

    fun buildVerifiedAppLink(host: String, imageId: String): String {
        val safeHost = host.trim().lowercase(Locale.US).removePrefix("https://").removePrefix("http://").trim('/').ifEmpty { "promptvault.app" }
        return "https://$safeHost/image/$imageId"
    }
}
