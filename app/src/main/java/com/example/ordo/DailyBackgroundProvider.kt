package com.example.ordo

import java.util.Calendar
import kotlin.random.Random

object DailyBackgroundProvider {
    fun getIndexForToday(): Int {
        val cal = Calendar.getInstance()
        cal.add(Calendar.HOUR_OF_DAY, -1)

        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)

        // Делим день года на 10 и берем остаток.
        // Результат будет строго по кругу: от 1 до 10.
        // Картинка гарантированно меняется на другую каждую ночь в 01:00.
        return (dayOfYear % 10) + 1
    }
}