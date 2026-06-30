package com.example.data

import android.util.Base64
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object VaultCrypto {
    private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding"
    private const val ITERATIONS = 2048
    private const val KEY_LENGTH = 256
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    /**
     * Generates a random salt for PBKDF2 key derivation.
     */
    fun generateSalt(size: Int = 16): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(size)
        random.nextBytes(salt)
        return salt
    }

    /**
     * Derives an AES-256 SecretKey from a passphrase and salt using PBKDF2.
     */
    fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKey {
        return try {
            val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
            val spec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_LENGTH)
            val tempKey = factory.generateSecret(spec)
            SecretKeySpec(tempKey.encoded, "AES")
        } catch (e: Exception) {
            throw RuntimeException("Key derivation failed: ${e.message}", e)
        }
    }

    /**
     * Encrypts plaintext using AES-256 GCM.
     * Returns a Pair of (Base64 encoded ciphertext, Base64 encoded IV)
     */
    fun encrypt(plainText: String, key: SecretKey): Pair<String, String> {
        val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)
        val cipherTextBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        
        val encryptedBase64 = Base64.encodeToString(cipherTextBytes, Base64.NO_WRAP)
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        
        return Pair(encryptedBase64, ivBase64)
    }

    /**
     * Decrypts AES-256 GCM ciphertext using the key and IV.
     * Throws exception if key or IV is invalid or ciphertext is tampered.
     */
    fun decrypt(cipherTextBase64: String, ivBase64: String, key: SecretKey): String {
        val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
        val cipherTextBytes = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
        val decryptedBytes = cipher.doFinal(cipherTextBytes)
        
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
