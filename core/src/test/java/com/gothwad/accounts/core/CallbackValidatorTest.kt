package com.gothwad.accounts.core

import org.junit.Assert.*
import org.junit.Test

class CallbackValidatorTest {
    private val redirect = "com.gothwad.accounts:/oauth/callback"
    @Test fun acceptsBoundCallback() {
        assertEquals("abc", CallbackValidator.parameters("$redirect?state=expected&code=abc", redirect, "expected")["code"])
    }
    @Test fun rejectsMismatchDuplicatesAndAmbiguity() {
        listOf(
            "$redirect?state=wrong&code=abc",
            "$redirect?state=expected&code=a&code=b",
            "$redirect?state=expected&code=a&error=denied",
            "$redirect?state=expected&code=a#fragment",
            "com.attacker:/oauth/callback?state=expected&code=a",
            "com.gothwad.accounts:/other?state=expected&code=a",
            "$redirect?state=expected&code=",
            "$redirect?state=expected&%73tate=expected&code=a",
        ).forEach { callback ->
            assertThrows(IllegalArgumentException::class.java) { CallbackValidator.parameters(callback, redirect, "expected") }
        }
    }
    @Test fun permitsBoundDenial() {
        assertEquals("access_denied", CallbackValidator.parameters("$redirect?state=expected&error=access_denied", redirect, "expected")["error"])
    }
    @Test fun requiresSafeConfiguration() {
        assertThrows(IllegalArgumentException::class.java) { GothwadConfig("client", redirect, authBaseUrl = "http://example.com/api") }
        assertThrows(IllegalArgumentException::class.java) { GothwadConfig("client", "javascript:alert(1)") }
        GothwadConfig("client", redirect)
        GothwadConfig("client", "https://accounts.example.com/oauth/callback")
    }
}
