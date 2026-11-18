package com.example

import com.example.util.UrlAndDeepLinkValidator
import com.example.util.UrlValidationResult
import com.example.viewmodel.AdminViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun directImageUrlValidator_acceptsValidImageExtensions() {
        val jpgResult = UrlAndDeepLinkValidator.validateDirectImageUrl("https://cdn.example.com/art/portrait.jpg")
        assertTrue(jpgResult is UrlValidationResult.Valid)
        assertEquals("JPG", (jpgResult as UrlValidationResult.Valid).detectedFormat)

        val webpResult = UrlAndDeepLinkValidator.validateDirectImageUrl("https://cdn.example.com/art/cyberpunk.webp")
        assertTrue(webpResult is UrlValidationResult.Valid)
        assertEquals("WEBP", (webpResult as UrlValidationResult.Valid).detectedFormat)
    }

    @Test
    fun directImageUrlValidator_rejectsFirebaseStorageAndNonImageLinks() {
        val storageResult = UrlAndDeepLinkValidator.validateDirectImageUrl("https://firebasestorage.googleapis.com/v0/b/demo/o/image.jpg")
        assertTrue(storageResult is UrlValidationResult.Invalid)

        val htmlResult = UrlAndDeepLinkValidator.validateDirectImageUrl("https://example.com/gallery/page.html")
        assertTrue(htmlResult is UrlValidationResult.Invalid)
    }

    @Test
    fun automaticDeepLinkGenerator_createsConsistentSchemeDestination() {
        val deepLink = UrlAndDeepLinkValidator.buildAutoDeepLink("promptvault", "img_neon_12345678")
        assertEquals("promptvault://image/img_neon_12345678", deepLink)
    }

    @Test
    fun adminEmailAllowlist_acceptsOnlyVerifiedAllowlistedGmail() {
        assertTrue(AdminViewModel.isAuthorizedAdminEmail("sfforhadvai@gmail.com", true))
        assertTrue(AdminViewModel.isAuthorizedAdminEmail("SFFORHADVAI@GMAIL.COM", true))
        assertFalse(AdminViewModel.isAuthorizedAdminEmail("sfforhadvai@gmail.com", false))
        assertFalse(AdminViewModel.isAuthorizedAdminEmail("intruder@gmail.com", true))
        assertFalse(AdminViewModel.isAuthorizedAdminEmail(null, true))
    }
}
