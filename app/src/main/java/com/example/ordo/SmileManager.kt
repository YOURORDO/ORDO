package com.example.ordo

import android.os.Build
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import android.content.Context
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

object SmileManager {
    // Теперь это пустая карта, которую мы заполним при запуске
    var smiles = mutableMapOf<String, String>()

    // Кэш пропорций (Ширина / Высота)
    private val aspectRatios = mutableMapOf<String, Float>()

    // Паттерн поиска кодов (теперь создается динамически)
    var smilePattern: Regex = "".toRegex()

    // Наш загрузчик GIF
    lateinit var imageLoader: ImageLoader

    suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        // --- ЗАВОД Coil ---
        imageLoader = ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
                else add(GifDecoder.Factory())
            }
            .build()

        // --- СКАНЕР ПАПКИ ASSETS ---
        try {
            val files = context.assets.list("smiles")
            if (files != null) {
                for (fileName in files) {
                    if (fileName.endsWith(".gif")) {
                        val code = ":" + fileName.substringBeforeLast(".") + ":"
                        smiles[code] = fileName
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // --- ОБНОВЛЯЕМ ПАТТЕРН ---
        smilePattern = smiles.keys.map { Regex.escape(it) }.joinToString("|").toRegex()

        // --- СКАНИРУЕМ РАЗМЕРЫ ---
        smiles.forEach { (code, fileName) ->
            try {
                val inputStream: InputStream = context.assets.open("smiles/$fileName")
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(inputStream, null, options)
                aspectRatios[code] = if (options.outWidth > 0) options.outWidth.toFloat() / options.outHeight else 1.0f
                inputStream.close()
            } catch (e: Exception) {
                aspectRatios[code] = 1.0f
            }
        }
    }

    fun getAspectRatio(code: String): Float = aspectRatios[code] ?: 1.0f

    // --- НОВЫЕ ФУНКЦИИ ДЛЯ СОРТИРОВКИ ПО ПОПУЛЯРНОСТИ ---
    private const val PREFS_NAME = "smile_usage_prefs"

    // 1. Записываем факт нажатия на смайл
    fun recordUsage(context: android.content.Context, code: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val currentCount = prefs.getInt(code, 0)
        prefs.edit().putInt(code, currentCount + 1).apply()
    }

    // 2. Получаем список смайлов, отсортированный по количеству нажатий
    fun getSortedSmiles(context: android.content.Context): List<Pair<String, String>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)

        // Превращаем карту смайлов в список и сортируем по убыванию нажатий
        return smiles.entries.map { it.key to it.value }
            .sortedByDescending { (code, _) ->
                prefs.getInt(code, 0)
            }
    }
}