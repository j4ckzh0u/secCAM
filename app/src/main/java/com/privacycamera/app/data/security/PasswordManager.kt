package com.privacycamera.app.data.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordManager @Inject constructor() {

    companion object {
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATIONS = 10000
        private const val KEY_LENGTH = 256
        private const val SALT_LENGTH = 16
    }

    private var storedHash: String? = null
    private var storedSalt: String? = null

    fun setPassword(password: String) {
        val salt = generateSalt()
        val hash = hashPassword(password, salt)
        storedSalt = salt
        storedHash = hash
    }

    fun setStoredCredentials(hash: String, salt: String) {
        storedHash = hash
        storedSalt = salt
    }

    fun verifyPassword(password: String): Boolean {
        val hash = storedHash ?: return false
        val salt = storedSalt ?: return false
        val inputHash = hashPassword(password, salt)
        return hash == inputHash
    }

    fun isPasswordSet(): Boolean {
        return storedHash != null && storedSalt != null
    }

    fun getStoredHash(): String? = storedHash

    fun getStoredSalt(): String? = storedSalt

    private fun generateSalt(): String {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    private fun hashPassword(password: String, salt: String): String {
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }
}
