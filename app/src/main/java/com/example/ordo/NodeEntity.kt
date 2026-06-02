package com.example.ordo

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nodes")
data class NodeEntity(
    @PrimaryKey val id: String, // Наш иерархический индекс соты (например, "1.2.1")
    val name: String,           // Название папки или чата
    val nodeType: String        // "STANDARD" (папка) или "CHAT" (зашифрованный чат)
)