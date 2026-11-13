package com.example.fund.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val DEFAULT_ITERATIONS = 10_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    private val secureRandom = SecureRandom()

    fun generateSalt(lengthBytes: Int = SALT_BYTES): String {
        val salt = ByteArray(lengthBytes)
        secureRandom.nextBytes(salt)
        return encodeBase64(salt)
    }

    /**
     * Hashes [password] with PBKDF2WithHmacSHA256 and a random per-user salt.
     * Format: "<iterations>:<base64Salt>:<base64Hash>"
     */
    fun hashPassword(
        password: String,
        saltBase64: String = generateSalt(),
        iterations: Int = DEFAULT_ITERATIONS
    ): String {
        val salt = decodeBase64(saltBase64)
        val derived = deriveKey(password, salt, iterations)
        return "$iterations:$saltBase64:${encodeBase64(derived)}"
    }

    /**
     * Constant-time verification of [password] against [storedHash].
     * Supports "<iterations>:<salt>:<hash>" and legacy "<salt>:<hash>".
     */
    fun verifyPassword(password: String, storedHash: String): Boolean {
        if (password.isEmpty() || storedHash.isBlank()) return false
        return try {
            val parts = storedHash.split(":")
            val iterations: Int
            val saltBase64: String
            val expectedHashBase64: String

            when (parts.size) {
                3 -> {
                    iterations = parts[0].toIntOrNull()?.takeIf { it > 0 } ?: return false
                    saltBase64 = parts[1]
                    expectedHashBase64 = parts[2]
                }
                2 -> {
                    iterations = DEFAULT_ITERATIONS
                    saltBase64 = parts[0]
                    expectedHashBase64 = parts[1]
                }
                else -> return false
            }

            val salt = decodeBase64(saltBase64)
            val expectedBytes = decodeBase64(expectedHashBase64)
            if (salt.isEmpty() || expectedBytes.isEmpty()) return false

            val computedBytes = deriveKey(password, salt, iterations)
            MessageDigest.isEqual(computedBytes, expectedBytes)
        } catch (_: Exception) {
            false
        }
    }

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun shortSha256(input: String): String {
        return sha256Hex(input).take(8).uppercase()
    }

    fun hmacSha256Hex(payload: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(keySpec)
        val raw = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        return raw.joinToString("") { "%02x".format(it) }
    }

    fun verifyPayloadIntegrity(payload: String, expectedSha256Hex: String): Boolean {
        if (expectedSha256Hex.isBlank()) return false
        val actual = sha256Hex(payload).toByteArray(Charsets.UTF_8)
        val expected = expectedSha256Hex.lowercase().toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(actual, expected)
    }

    // Pure-Kotlin RFC 4648 Base64 codec (works on Android minSdk 24+ and pure JVM JUnit tests)
    private const val B64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    internal fun encodeBase64(input: ByteArray): String {
        if (input.isEmpty()) return ""
        val out = StringBuilder(((input.size + 2) / 3) * 4)
        var i = 0
        while (i < input.size) {
            val b0 = input[i].toInt() and 0xFF
            val hasB1 = i + 1 < input.size
            val hasB2 = i + 2 < input.size
            val b1 = if (hasB1) input[i + 1].toInt() and 0xFF else 0
            val b2 = if (hasB2) input[i + 2].toInt() and 0xFF else 0

            val c0 = (b0 ushr 2) and 0x3F
            val c1 = ((b0 and 0x03) shl 4) or ((b1 ushr 4) and 0x0F)
            val c2 = ((b1 and 0x0F) shl 2) or ((b2 ushr 6) and 0x03)
            val c3 = b2 and 0x3F

            out.append(B64_ALPHABET[c0])
            out.append(B64_ALPHABET[c1])
            out.append(if (hasB1) B64_ALPHABET[c2] else '=')
            out.append(if (hasB2) B64_ALPHABET[c3] else '=')
            i += 3
        }
        return out.toString()
    }

    internal fun decodeBase64(input: String): ByteArray {
        val clean = input.trim().filterNot { it.isWhitespace() }
        if (clean.isEmpty() || clean.length % 4 != 0) return ByteArray(0)

        var padding = 0
        if (clean.endsWith("==")) padding = 2
        else if (clean.endsWith("=")) padding = 1

        val outLen = (clean.length / 4) * 3 - padding
        val out = ByteArray(outLen)
        var outIdx = 0
        var i = 0
        while (i < clean.length) {
            val c0 = B64_ALPHABET.indexOf(clean[i])
            val c1 = B64_ALPHABET.indexOf(clean[i + 1])
            val c2 = if (clean[i + 2] == '=') 0 else B64_ALPHABET.indexOf(clean[i + 2])
            val c3 = if (clean[i + 3] == '=') 0 else B64_ALPHABET.indexOf(clean[i + 3])
            if (c0 < 0 || c1 < 0 || c2 < 0 || c3 < 0) return ByteArray(0)

            val triple = (c0 shl 18) or (c1 shl 12) or (c2 shl 6) or c3
            if (outIdx < outLen) out[outIdx++] = ((triple ushr 16) and 0xFF).toByte()
            if (outIdx < outLen) out[outIdx++] = ((triple ushr 8) and 0xFF).toByte()
            if (outIdx < outLen) out[outIdx++] = (triple and 0xFF).toByte()
            i += 4
        }
        return out
    }
}
