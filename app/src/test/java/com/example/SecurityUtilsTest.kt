package com.example

import com.example.fund.security.SecurityUtils
import org.junit.Assert.*
import org.junit.Test

class SecurityUtilsTest {

    @Test
    fun `pbkdf2 uses unique per-user salts and verifies matching password only`() {
        val hash1 = SecurityUtils.hashPassword("Secret#2026")
        val hash2 = SecurityUtils.hashPassword("Secret#2026")

        // Random salts ensure identical passwords produce distinct stored hashes
        assertNotEquals(hash1, hash2)
        assertEquals(3, hash1.split(":").size)

        assertTrue(SecurityUtils.verifyPassword("Secret#2026", hash1))
        assertTrue(SecurityUtils.verifyPassword("Secret#2026", hash2))

        assertFalse(SecurityUtils.verifyPassword("WrongPass", hash1))
        assertFalse(SecurityUtils.verifyPassword("", hash1))
        assertFalse(SecurityUtils.verifyPassword("Secret#2026", "malformed-hash"))
    }

    @Test
    fun `sha256 and payload integrity verification work deterministically`() {
        val payload = """{"version":"9.0","total":253000}"""
        val digest = SecurityUtils.sha256Hex(payload)
        assertEquals(64, digest.length)
        assertTrue(SecurityUtils.verifyPayloadIntegrity(payload, digest))
        assertFalse(SecurityUtils.verifyPayloadIntegrity("$payload ", digest))

        val hmac = SecurityUtils.hmacSha256Hex(payload, "node-key-1")
        assertEquals(64, hmac.length)
        assertNotEquals(digest, hmac)
    }
}
