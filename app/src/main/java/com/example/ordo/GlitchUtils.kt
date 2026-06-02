package com.example.ordo

import kotlin.random.Random

fun generateGlitchText(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_#<>[]"
    return (1..5).map { chars[Random.nextInt(chars.length)] }.joinToString("")
}