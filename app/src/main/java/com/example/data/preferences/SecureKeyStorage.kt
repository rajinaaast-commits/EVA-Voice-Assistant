package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureKeyStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("eva_secure_vault", Context.MODE_PRIVATE)
    private val keyAlias = "EVA_VAULT_KEY"
    private val androidKeyStore = "AndroidKeyStore"
    private val transformation = "AES/GCM/NoPadding"

    init {
        initKeyStore()
    }

    private fun initKeyStore() {
        try {
            val keyStore = KeyStore.getInstance(androidKeyStore).apply { load(null) }
            if (!keyStore.containsAlias(keyAlias)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    androidKeyStore
                )
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (_: Exception) {
            // Fallback gracefully if hardware keystore has limitations
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(androidKeyStore).apply { load(null) }
            val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
            entry?.secretKey
        } catch (_: Exception) {
            null
        }
    }

    fun saveEncrypted(key: String, value: String) {
        if (value.isBlank()) {
            prefs.edit().remove(key).remove("${key}_iv").apply()
            return
        }
        val secretKey = getSecretKey()
        if (secretKey != null) {
            try {
                val cipher = Cipher.getInstance(transformation)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val iv = cipher.iv
                val encryptedBytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
                prefs.edit()
                    .putString(key, Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
                    .putString("${key}_iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                    .apply()
                return
            } catch (_: Exception) {
                // fallback to obfuscated base64 storage if cipher fails
            }
        }
        // Fallback obfuscation
        val obfuscated = Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        prefs.edit().putString(key, obfuscated).remove("${key}_iv").apply()
    }

    fun getDecrypted(key: String): String {
        val encodedData = prefs.getString(key, null) ?: return ""
        val encodedIv = prefs.getString("${key}_iv", null)
        val secretKey = getSecretKey()

        if (secretKey != null && encodedIv != null) {
            try {
                val iv = Base64.decode(encodedIv, Base64.NO_WRAP)
                val cipher = Cipher.getInstance(transformation)
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                val decryptedBytes = cipher.doFinal(Base64.decode(encodedData, Base64.NO_WRAP))
                return String(decryptedBytes, Charsets.UTF_8)
            } catch (_: Exception) {
                // fall through to check if plain encoded
            }
        }

        return try {
            String(Base64.decode(encodedData, Base64.NO_WRAP), Charsets.UTF_8)
        } catch (_: Exception) {
            encodedData
        }
    }

    fun clearKey(key: String) {
        prefs.edit().remove(key).remove("${key}_iv").apply()
    }

    companion object {
        const val KEY_GEMINI = "gemini_api_key"
        const val KEY_OMNIROUTE = "omniroute_api_key"
        const val KEY_OMNIROUTE_URL = "omniroute_base_url"
    }
}
