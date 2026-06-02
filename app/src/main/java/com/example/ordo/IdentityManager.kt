package com.example.ordo

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.security.MessageDigest
import java.security.SecureRandom

object IdentityManager {
    private const val KEY_MY_NAME = "my_name"
    private const val KEY_MY_AVATAR = "my_avatar"
    private const val PREFS_NAME = "identity_vault_v3"
    private const val KEY_PRIV = "priv_key"
    private const val KEY_PUB = "pub_key"

    private var privateKey: Ed25519PrivateKeyParameters? = null
    private var publicKey: Ed25519PublicKeyParameters? = null

    const val infoChannelPubKey = "07c5e145c834b97a477e1923e6069086c96f8b4629887ba437e0f9a4088cc68c"

    private val words = listOf(
        "Steel", "Iron", "Mercury", "Wood", "Copper",
        "Gold", "Silver", "Diamond", "Emerald", "Obsidian", "Quartz"
    )

    // ЧИСТОЕ ПОЛУЧЕНИЕ ШИФРОВАННЫХ НАСТРОЕК (без опасных фоновых откатов)
    private fun getEncryptedPrefs(context: Context): android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveProfile(context: Context, name: String, avatarBase64: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_MY_NAME, name)
            putString(KEY_MY_AVATAR, avatarBase64)
            apply()
        }
    }

    fun getMyName(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_MY_NAME, "")
            ?: ""

    fun getMyAvatar(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MY_AVATAR, null)

    // БЕЗОПАСНАЯ ИНИЦИАЛИЗАЦИЯ
    fun init(context: Context) {
        if (privateKey != null && publicKey != null) return

        try {
            val prefs = getEncryptedPrefs(context)
            val savedPriv = prefs.getString(KEY_PRIV, null)
            val savedPub = prefs.getString(KEY_PUB, null)

            if (savedPriv != null && savedPub != null) {
                privateKey = Ed25519PrivateKeyParameters(Base64.decode(savedPriv, Base64.NO_WRAP), 0)
                publicKey = Ed25519PublicKeyParameters(Base64.decode(savedPub, Base64.NO_WRAP), 0)
            } else {
                // Генерируем только если файл успешно открылся, но ключей в нем еще нет (первый запуск)
                generateAndSaveKeys(prefs)
            }
        } catch (e: Exception) {
            // Если во сне произошел временный сбой Keystore — мы просто пишем ошибку и выходим.
            // Мы НЕ генерируем новые ключи в обычных настройках, сохраняя целостность базы данных!
            android.util.Log.e("IDENTITY_VAULT", "Временный сбой Keystore во сне (пропускаем шаг): ${e.message}")
        }
    }

    private fun generateAndSaveKeys(prefs: android.content.SharedPreferences) {
        val gen = Ed25519KeyPairGenerator()
        gen.init(Ed25519KeyGenerationParameters(SecureRandom()))
        val pair = gen.generateKeyPair()

        privateKey = pair.private as Ed25519PrivateKeyParameters
        publicKey = pair.public as Ed25519PublicKeyParameters

        prefs.edit().apply {
            putString(KEY_PRIV, Base64.encodeToString(privateKey!!.encoded, Base64.NO_WRAP))
            putString(KEY_PUB, Base64.encodeToString(publicKey!!.encoded, Base64.NO_WRAP))
            apply()
        }
    }

    fun getFullPublicKey(): String = publicKey?.encoded?.toHex() ?: ""

    fun getDbKey(): String = privateKey?.encoded?.toHex() ?: "default_fallback_key"


    fun isSystemInfo(): Boolean = getFullPublicKey() == infoChannelPubKey

    fun getShortId(): String {
        val pubHex = getFullPublicKey()
        if (pubHex == infoChannelPubKey) return "FirstOrdo"

        if (pubHex.length < 16) return "Unknown"
        return try {
            val wordPart = pubHex.substring(0, 8).toLong(16)
            val digitPart = pubHex.substring(8, 16).toLong(16)
            val wordIndex = (wordPart % 11).toInt()
            val digits = (digitPart % 1000000).toString().padStart(6, '0')
            "${words[wordIndex]}$digits"
        } catch (e: Exception) {
            pubHex.take(8).uppercase()
        }
    }

    fun sign(payload: String): String {
        val signer = Ed25519Signer()
        signer.init(true, privateKey)
        val data = payload.toByteArray(Charsets.UTF_8)
        signer.update(data, 0, data.size)
        return signer.generateSignature().toHex()
    }

    fun createChannelId(myId: String, partnerId: String, secret: String): String {
        val sortedIds = listOf(myId.uppercase(), partnerId.uppercase()).sorted()
        val input = "${sortedIds[0]}|${sortedIds[1]}|$secret"
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray()).toHex().take(32)
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    fun createQrData(customServer: String): String {
        val timestamp = System.currentTimeMillis()
        val payload = "PORYADOK_V1|${getFullPublicKey()}|$timestamp|$customServer"
        val signature = sign(payload)
        return "$payload|$signature"
    }

    fun parseQrData(data: String): Pair<String, String>? {
        return try {
            val parts = data.split("|")
            if (parts.size != 5 || parts[0] != "PORYADOK_V1") return null

            val pubKeyHex = parts[1]
            val timestamp = parts[2].toLong()
            val serverUrl = parts[3]
            val signatureHex = parts[4]

            val payloadToVerify = "PORYADOK_V1|$pubKeyHex|$timestamp|$serverUrl"

            val pubKeyBytes = pubKeyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

            val signer = org.bouncycastle.crypto.signers.Ed25519Signer()
            signer.init(
                false,
                org.bouncycastle.crypto.params.Ed25519PublicKeyParameters(pubKeyBytes, 0)
            )

            val payloadBytes = payloadToVerify.toByteArray(Charsets.UTF_8)
            signer.update(payloadBytes, 0, payloadBytes.size)

            val sigBytes = signatureHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            if (!signer.verifySignature(sigBytes)) {
                return null
            }

            val now = System.currentTimeMillis()
            if (now - timestamp > 30 * 60 * 1000) Pair("EXPIRED", "") else Pair(pubKeyHex, serverUrl)
        } catch (e: Exception) {
            null
        }
    }

    fun exportIdentity(context: Context): String? {
        return try {
            val prefs = getEncryptedPrefs(context)
            val savedPriv = prefs.getString(KEY_PRIV, null)
            val savedPub = prefs.getString(KEY_PUB, null)

            if (savedPriv != null && savedPub != null) {
                "$savedPriv|$savedPub"
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun importIdentity(context: Context, backupString: String): Boolean {
        try {
            val parts = backupString.split("|")
            if (parts.size != 2) return false

            val privBase64 = parts[0]
            val pubBase64 = parts[1]

            val newPriv = org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters(android.util.Base64.decode(privBase64, android.util.Base64.NO_WRAP), 0)
            val newPub = org.bouncycastle.crypto.params.Ed25519PublicKeyParameters(android.util.Base64.decode(pubBase64, android.util.Base64.NO_WRAP), 0)


            val prefs = try {
                getEncryptedPrefs(context)
            } catch (e: Exception) {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }

            prefs.edit().apply {
                putString(KEY_PRIV, privBase64)
                putString(KEY_PUB, pubBase64)
                apply()
            }

            privateKey = newPriv
            publicKey = newPub

            try {
                DatabaseManager.closeDatabase()
            } catch (e: Exception) {}

            context.deleteDatabase("poryadok_vault.db")

            try {
                DatabaseManager.init(context)
            } catch (e: Exception) {
                android.util.Log.e("IDENTITY_IMPORT", "Не удалось перезапустить БД: ${e.message}")
            }

            return true
        } catch (e: Exception) {
            return false
        }
    }
}