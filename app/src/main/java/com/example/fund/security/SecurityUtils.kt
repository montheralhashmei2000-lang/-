package com.example.fund.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricPrompt
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    const val HASH_ALGORITHM_VERSION = "pbkdf2_sha256_v2"
    const val DEFAULT_ITERATIONS = 120_000
    private const val LEGACY_DEFAULT_ITERATIONS = 10_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    private const val AES_GCM_TRANSFORM = "AES/GCM/NoPadding"
    private const val AES_GCM_PREFIX = "AES256GCM_V1:"
    private const val GCM_IV_BYTES = 12
    private const val GCM_TAG_BITS = 128

    private val secureRandom = SecureRandom()

    fun generateSalt(lengthBytes: Int = SALT_BYTES): String {
        val salt = ByteArray(lengthBytes)
        secureRandom.nextBytes(salt)
        return encodeBase64(salt)
    }

    fun generateRandomKeyBytes(lengthBytes: Int = 32): ByteArray {
        val key = ByteArray(lengthBytes)
        secureRandom.nextBytes(key)
        return key
    }

    /**
     * Hashes [password] with PBKDF2WithHmacSHA256 using 120,000 iterations and a random per-user salt.
     * Format: "pbkdf2_sha256_v2$<iterations>:<base64Salt>:<base64Hash>"
     */
    fun hashPassword(
        password: String,
        saltBase64: String = generateSalt(),
        iterations: Int = DEFAULT_ITERATIONS
    ): String {
        val salt = decodeBase64(saltBase64)
        val derived = deriveKey(password, salt, iterations)
        return "$HASH_ALGORITHM_VERSION$$iterations:$saltBase64:${encodeBase64(derived)}"
    }

    /**
     * Constant-time verification of [password] against [storedHash].
     * Supports:
     *  - "pbkdf2_sha256_v2$<iterations>:<salt>:<hash>"
     *  - "pbkdf2_sha256_v2$<iterations>$<salt>$<hash>"
     *  - Legacy "<iterations>:<salt>:<hash>"
     *  - Legacy "<salt>:<hash>"
     */
    fun verifyPassword(password: String, storedHash: String): Boolean {
        if (password.isEmpty() || storedHash.isBlank()) return false
        return try {
            val iterations: Int
            val saltBase64: String
            val expectedHashBase64: String

            val colonParts = storedHash.split(":")
            if (colonParts.size == 3) {
                val iterToken = colonParts[0]
                iterations = if (iterToken.startsWith("$HASH_ALGORITHM_VERSION$")) {
                    iterToken.substringAfter("$HASH_ALGORITHM_VERSION$").toIntOrNull()?.takeIf { it > 0 } ?: return false
                } else {
                    iterToken.toIntOrNull()?.takeIf { it > 0 } ?: return false
                }
                saltBase64 = colonParts[1]
                expectedHashBase64 = colonParts[2]
            } else if (colonParts.size == 2) {
                iterations = LEGACY_DEFAULT_ITERATIONS
                saltBase64 = colonParts[0]
                expectedHashBase64 = colonParts[1]
            } else {
                val dollarParts = storedHash.split("$")
                if (dollarParts.size == 4 && dollarParts[0] == HASH_ALGORITHM_VERSION) {
                    iterations = dollarParts[1].toIntOrNull()?.takeIf { it > 0 } ?: return false
                    saltBase64 = dollarParts[2]
                    expectedHashBase64 = dollarParts[3]
                } else {
                    return false
                }
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

    fun deriveAesKeyFromSecret(secret: String): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(secret.toByteArray(Charsets.UTF_8))
    }

    fun encryptAesGcm(plaintext: String, keyBytes: ByteArray): String {
        val iv = ByteArray(GCM_IV_BYTES)
        secureRandom.nextBytes(iv)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORM)
        val keySpec = SecretKeySpec(keyBytes, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)
        return AES_GCM_PREFIX + encodeBase64(combined)
    }

    fun decryptAesGcm(envelope: String, keyBytes: ByteArray): String? {
        if (!envelope.startsWith(AES_GCM_PREFIX)) return null
        return try {
            val raw = decodeBase64(envelope.removePrefix(AES_GCM_PREFIX))
            if (raw.size <= GCM_IV_BYTES) return null
            val iv = raw.copyOfRange(0, GCM_IV_BYTES)
            val cipherBytes = raw.copyOfRange(GCM_IV_BYTES, raw.size)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORM)
            val keySpec = SecretKeySpec(keyBytes, "AES")
            cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (_: Exception) {
            null
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

/**
 * Hardware-backed AndroidKeyStore AES-256-GCM CryptoObject manager bound to a specific [userId].
 */
object BiometricKeyStoreHelper {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS_PREFIX = "social_fund_bio_key_"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun keyAliasFor(userId: String): String = "$KEY_ALIAS_PREFIX${userId.trim()}"

    fun getOrCreateBiometricCipher(userId: String): BiometricPrompt.CryptoObject? {
        if (userId.isBlank()) return null
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val alias = keyAliasFor(userId)
            if (!keyStore.containsAlias(alias)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val spec = KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(true)
                    .setInvalidatedByBiometricEnrollment(true)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
            val secretKey = keyStore.getKey(alias, null) as? SecretKey ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            BiometricPrompt.CryptoObject(cipher)
        } catch (_: Exception) {
            null
        }
    }

    fun verifyUnlockedCrypto(result: BiometricPrompt.AuthenticationResult, userId: String): Boolean {
        val cipher = result.cryptoObject?.cipher ?: return false
        return try {
            val sealed = cipher.doFinal("AUTH:$userId:${System.currentTimeMillis()}".toByteArray(Charsets.UTF_8))
            sealed.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }
}
