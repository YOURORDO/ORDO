package com.example.ordo

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionManager {
    private const val ALGORITHM = "AES/GCM/NoPadding"

    // Создаем непробиваемый ключ из Секретного слова и ID
    // Создаем непробиваемый ключ из Секретного слова и ID
    fun generateKey(secretWord: String, pub1: String, pub2: String): String {
        // ПРИВОДИМ К ВЕРХНЕМУ РЕГИСТРУ ОБА ID ПЕРЕД СОРТИРОВКОЙ
        val sorted = listOf(pub1.uppercase(), pub2.uppercase()).sorted()

        // Надежная генерация ключа (PBKDF2). Защита от перебора (Brute-force).
        val password = secretWord.toCharArray()
        val salt = "${sorted[0]}|${sorted[1]}".toByteArray(Charsets.UTF_8)
        val iterations = 100000 // 100 тысяч итераций - замедлят хакера в миллионы раз
        val keyLength = 256 // 256 бит для AES-256

        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = javax.crypto.spec.PBEKeySpec(password, salt, iterations, keyLength)
        val keyBytes = factory.generateSecret(spec).encoded

        return Base64.encodeToString(keyBytes, Base64.NO_WRAP)
    }

    fun encrypt(text: String, keyBase64: String): String {
        if (text.startsWith("[SIGNAL_")) return text // Сигналы Эфира не шифруем, они нужны серверу!

        val keyBytes = Base64.decode(keyBase64, Base64.NO_WRAP)
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv) // Случайная соль для каждого сообщения
        val gcmSpec = GCMParameterSpec(128, iv)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val cipherText = cipher.doFinal(text.toByteArray(Charsets.UTF_8))

        // Склеиваем соль и зашифрованный текст
        val combined = iv + cipherText
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(base64Text: String, keyBase64: String): String {
        if (base64Text.startsWith("[SIGNAL_")) return base64Text

        return try {
            val keyBytes = Base64.decode(keyBase64, Base64.NO_WRAP)
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val combined = Base64.decode(base64Text, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, 12)
            val cipherText = combined.copyOfRange(12, combined.size)

            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val plainText = cipher.doFinal(cipherText)
            String(plainText, Charsets.UTF_8)
        } catch (e: Exception) {
            "🛡[Зашифрованное сообщение]" // Если ключ не подошел, покажем это
        }
    }
}