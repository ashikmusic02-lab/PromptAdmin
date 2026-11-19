package com.example

import com.example.model.AdminRole
import com.example.model.AdminSection
import com.example.util.UrlAndDeepLinkValidator
import com.example.util.UrlValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun googleDriveLinks_areNormalizedToDirectViewUrl() {
        val shareUrl = "https://drive.google.com/file/d/1AbCdEfGhIjKlMnOpQrStUvWxYz/view?usp=sharing"
        assertEquals(
            "https://drive.google.com/uc?export=view&id=1AbCdEfGhIjKlMnOpQrStUvWxYz",
            UrlAndDeepLinkValidator.normalizePublicImageUrl(shareUrl)
        )

        val openUrl = "https://drive.google.com/open?id=1AbCdEfGhIjKlMnOpQrStUvWxYz"
        assertEquals(
            "https://drive.google.com/uc?export=view&id=1AbCdEfGhIjKlMnOpQrStUvWxYz",
            UrlAndDeepLinkValidator.normalizePublicImageUrl(openUrl)
        )
    }

    @Test
    fun publicImageUrlValidator_acceptsExtensionlessAndDriveUrls() {
        val driveRes = UrlAndDeepLinkValidator.validatePublicImageUrl(
            "https://drive.google.com/file/d/1AbCdEfGhIjKlMnOpQrStUvWxYz/view"
        )
        assertTrue(driveRes is UrlValidationResult.Valid)

        val extensionlessRes = UrlAndDeepLinkValidator.validatePublicImageUrl(
            "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1200"
        )
        assertTrue(extensionlessRes is UrlValidationResult.Valid)
    }

    @Test
    fun automaticDeepLinkGenerator_createsPromptLensScheme() {
        val deepLink = UrlAndDeepLinkValidator.buildAutoDeepLink("img_neon_12345678")
        assertEquals("promptlens://image/img_neon_12345678", deepLink)
    }

    @Test
    fun adminRoles_mapToBanglaBadgesAndEnforceModeratorAccess() {
        val owner = AdminRole.fromFirestoreValue("owner")
        val admin = AdminRole.fromFirestoreValue("admin")
        val moderator = AdminRole.fromFirestoreValue("moderator")

        assertEquals("মালিক", owner?.banglaBadge)
        assertEquals("অ্যাডমিন", admin?.banglaBadge)
        assertEquals("মডারেটর", moderator?.banglaBadge)

        assertTrue(moderator!!.canAccessSection(AdminSection.COMMENTS_REPORTS))
        assertFalse(moderator.canAccessSection(AdminSection.IMAGES))
        assertFalse(moderator.canAccessSection(AdminSection.SETTINGS))
    }
}
