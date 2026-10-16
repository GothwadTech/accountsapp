package com.gothwad.accounts.core

import org.junit.Assert.*
import org.junit.Test

class PkceTest {
    @Test fun rfc7636Vector() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", Pkce.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
    }
    @Test fun verifierIsUnpaddedUrlSafeAndUnique() {
        val values = (1..100).map { Pkce.random() }
        assertTrue(values.all { it.matches(Regex("[A-Za-z0-9_-]{43}")) })
        assertEquals(100, values.toSet().size)
    }
}
