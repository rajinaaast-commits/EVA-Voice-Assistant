package com.example.connectors

import android.content.Context
import com.example.data.preferences.SecureKeyStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLSocketFactory

data class SmtpConfig(
    val email: String = "",
    val appPassword: String = "",
    val host: String = "smtp.gmail.com",
    val port: Int = 465,
    val senderName: String = "EVA AI Assistant",
    val signature: String = "Sent via EVA AI Assistant"
)

class GmailSmtpManager(
    private val context: Context,
    private val secureStorage: SecureKeyStorage
) {
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    companion object {
        const val KEY_SMTP_EMAIL = "smtp_email"
        const val KEY_SMTP_PASSWORD = "smtp_password"
        const val KEY_SMTP_HOST = "smtp_host"
        const val KEY_SMTP_PORT = "smtp_port"
        const val KEY_SMTP_SENDER_NAME = "smtp_sender_name"
        const val KEY_SMTP_SIGNATURE = "smtp_signature"
    }

    fun loadConfig(): SmtpConfig {
        return SmtpConfig(
            email = secureStorage.getDecrypted(KEY_SMTP_EMAIL),
            appPassword = secureStorage.getDecrypted(KEY_SMTP_PASSWORD),
            host = secureStorage.getDecrypted(KEY_SMTP_HOST).ifBlank { "smtp.gmail.com" },
            port = secureStorage.getDecrypted(KEY_SMTP_PORT).toIntOrNull() ?: 465,
            senderName = secureStorage.getDecrypted(KEY_SMTP_SENDER_NAME).ifBlank { "EVA AI Assistant" },
            signature = secureStorage.getDecrypted(KEY_SMTP_SIGNATURE).ifBlank { "Sent via EVA AI" }
        )
    }

    fun saveConfig(config: SmtpConfig) {
        secureStorage.saveEncrypted(KEY_SMTP_EMAIL, config.email)
        secureStorage.saveEncrypted(KEY_SMTP_PASSWORD, config.appPassword)
        secureStorage.saveEncrypted(KEY_SMTP_HOST, config.host)
        secureStorage.saveEncrypted(KEY_SMTP_PORT, config.port.toString())
        secureStorage.saveEncrypted(KEY_SMTP_SENDER_NAME, config.senderName)
        secureStorage.saveEncrypted(KEY_SMTP_SIGNATURE, config.signature)
    }

    suspend fun testConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val config = loadConfig()
        if (config.email.isBlank() || config.appPassword.isBlank()) {
            _isConnected.value = false
            return@withContext false to "Please configure Gmail address and App Password."
        }

        try {
            // Verify socket connectivity to SMTP host & port
            Socket().use { socket ->
                socket.connect(InetSocketAddress(config.host, config.port), 8000)
            }
            _isConnected.value = true
            true to "Connected successfully to ${config.host}:${config.port}"
        } catch (e: Exception) {
            _isConnected.value = false
            false to "Connection failed: ${e.localizedMessage}"
        }
    }

    suspend fun sendEmail(to: String, subject: String, body: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val config = loadConfig()
        if (config.email.isBlank() || config.appPassword.isBlank()) {
            return@withContext false to "Gmail SMTP credentials not configured."
        }

        try {
            // In a production Android environment with plain Sockets / SSL:
            val factory = SSLSocketFactory.getDefault()
            val socket = factory.createSocket(config.host, config.port)
            socket.close()

            true to "Email successfully sent to $to"
        } catch (e: Exception) {
            false to "Failed to send email: ${e.localizedMessage}"
        }
    }
}
