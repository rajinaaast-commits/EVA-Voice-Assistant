package com.example

import com.example.ai.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GeminiKeyValidationTest {

    private val provider = GeminiProvider()
    private val validator = GeminiKeyValidator(provider)

    @Test
    fun testValidKeysWithUnderscoreHyphenAndPeriodAreAccepted() {
        // Keys with underscore, hyphen, and period
        val keyWithSpecialChars = "AIzaSyD-example_key.with-special_chars.123"
        val syntaxResult = validator.validateSyntax(keyWithSpecialChars)

        assertTrue(
            "Key with underscore, hyphen, and period must be valid",
            syntaxResult is SyntaxValidation.Valid
        )
        assertEquals(keyWithSpecialChars, (syntaxResult as SyntaxValidation.Valid).cleanedKey)
    }

    @Test
    fun testSanitizeKeyTrimsOnlySurroundingWhitespace() {
        val rawPastedKey = "  \n\t AIzaSyD-example_key.123 \t\n  "
        val cleaned = validator.sanitizeKey(rawPastedKey)

        assertEquals("AIzaSyD-example_key.123", cleaned)
        // Ensure internal characters are not modified
        assertEquals("AIzaSyD-example_key.123", validator.sanitizeKey("AIzaSyD-example_key.123"))
    }

    @Test
    fun testEmptyKeyHandling() {
        val emptySyntax = validator.validateSyntax("")
        assertTrue("Empty key must be recognized as Missing", emptySyntax is SyntaxValidation.Missing)

        val whitespaceSyntax = validator.validateSyntax("   \n\t  ")
        assertTrue("Blank key must be recognized as Missing", whitespaceSyntax is SyntaxValidation.Missing)
    }

    @Test
    fun testSurroundingWhitespaceDetected() {
        val rawPastedWithSpaces = " AIzaSy_valid_key "
        val syntax = validator.validateSyntax(rawPastedWithSpaces)

        assertTrue(
            "Surrounding whitespace should be flagged as Malformed for cleaning",
            syntax is SyntaxValidation.Malformed
        )
        // Once cleaned, syntax is Valid
        val clean = validator.sanitizeKey(rawPastedWithSpaces)
        val cleanSyntax = validator.validateSyntax(clean)
        assertTrue(cleanSyntax is SyntaxValidation.Valid)
    }

    @Test
    fun testInternalWhitespaceFlagged() {
        val brokenKey = "AIzaSy key with internal spaces"
        val syntax = validator.validateSyntax(brokenKey)

        assertTrue(
            "Internal whitespace should be flagged as Malformed",
            syntax is SyntaxValidation.Malformed
        )
    }

    @Test
    fun testPlaceholderValuesFlagged() {
        val placeholders = listOf("YOUR_API_KEY", "YOUR_GEMINI_KEY", "API_KEY", "TODO", "REPLACE_ME")
        for (placeholder in placeholders) {
            val syntax = validator.validateSyntax(placeholder)
            assertTrue("Placeholder '$placeholder' should be flagged", syntax is SyntaxValidation.Malformed)
        }
    }

    @Test
    fun testNewlyGeneratedKeysOfVariousLengthsValidate() {
        val key39 = "AIzaSyD1234567890abcdefghijklmnopqrstuv"
        val keyShort = "AIzaSy_test-123"
        val keyLong = "AIzaSyD1234567890abcdefghijklmnopqrstuvwxyz_0123456789-ABC.DEF"

        assertTrue(validator.validateSyntax(key39) is SyntaxValidation.Valid)
        assertTrue(validator.validateSyntax(keyShort) is SyntaxValidation.Valid)
        assertTrue(validator.validateSyntax(keyLong) is SyntaxValidation.Valid)
    }

    @Test
    fun testInvalidKeyReturnsProperErrorMessage() = runBlocking {
        // A dummy non-existent key tested against Google Gemini servers
        val result = validator.validateKeyWithServer("AIzaSyFakeKey_12345-not-real.dummy", maxRetries = 1)
        if (result is KeyValidationState.Failed) {
            // Either authentication rejection from Google ("Gemini API key is invalid or revoked.")
            // or network error if offline
            if (result.reason == "Authentication Error") {
                assertEquals("Gemini API key is invalid or revoked.", result.userFriendlyMessage)
            } else {
                assertTrue(result.userFriendlyMessage.isNotBlank())
            }
        }
    }
}
