package com.example.ordo

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import androidx.room.Room

// Описываем безопасный переход: просто достраиваем пустую таблицу nodes на диске, не трогая старые данные
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `nodes` (" +
                    "`id` TEXT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`nodeType` TEXT NOT NULL, " +
                    "PRIMARY KEY(`id`))"
        )
    }
}

object DatabaseManager {
    private var db: AppDatabase? = null

    fun init(context: Context) {
        if (db != null) return

        val dbKey = IdentityManager.getDbKey()
        if (dbKey == "default_fallback_key") {
            throw IllegalStateException("Критическая ошибка: IdentityManager не проинициализирован, ключ базы отсутствует!")
        }

        System.loadLibrary("sqlcipher")

        val dbPassphrase = dbKey.toByteArray(Charsets.UTF_8)
        val factory = net.zetetic.database.sqlcipher.SupportOpenHelperFactory(dbPassphrase)

        db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java, "poryadok_vault.db"
        )
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_9_10) // <-- ПОДКЛЮЧИЛИ НАШ МОСТ МИГРАЦИИ!
            .build()
    }

    fun getDao() = db?.slotDao() ?: throw Exception("DB not initialized")

    fun getMsgDao() = db?.messageDao() ?: throw Exception("DB not initialized")

    // Метод доступа к новому DAO папок из любого места приложения
    fun getNodeDao() = db?.nodeDao() ?: throw Exception("DB not initialized")

    fun closeDatabase() {
        if (db?.isOpen == true) {
            db?.close()
        }
        db = null
    }
}