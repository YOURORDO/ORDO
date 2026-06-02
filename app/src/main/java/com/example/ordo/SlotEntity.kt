package com.example.ordo

import androidx.room.*

@Entity(tableName = "slots")
data class SlotEntity(
    @PrimaryKey val id: String,
    val partnerId: String = "",
    val channelId: String = "",
    val status: String = "EMPTY",
    val isEtherOn: Boolean = false,
    val aesKey: String = "",
    val partnerDisplayName: String = "",
    val partnerAvatarBase64: String? = null,
    val isMuted: Boolean = false,
    val hasActivePing: Boolean = false,
    val serverUrl: String = "" // --- НОВОЕ ПОЛЕ: АДРЕС СЕРВЕРА ---
)

@Dao
interface SlotDao {
    @Query("SELECT * FROM slots ORDER BY id ASC")
    fun getAllSlotsFlow(): kotlinx.coroutines.flow.Flow<List<SlotEntity>>

    @Query("SELECT * FROM slots ORDER BY id ASC")
    suspend fun getAllSlots(): List<SlotEntity>

    // --- СТАРЫЙ МЕТОД (ОБЯЗАТЕЛЬНО НУЖЕН) ---
    @Query("SELECT * FROM slots WHERE id = :slotId")
    suspend fun getSlotById(slotId: String): SlotEntity?

    // --- НОВЫЙ МЕТОД (КОТОРЫЙ МЫ ДОБАВИЛИ ДЛЯ ПИНГА) ---
    @Query("SELECT * FROM slots WHERE channelId = :chanId LIMIT 1")
    suspend fun getSlotByChannelId(chanId: String): SlotEntity?

    @Update
    suspend fun updateSlot(slot: SlotEntity)

    // НОВЫЙ МЕТОД ДЛЯ СОЗДАНИЯ ОДНОГО СЛОТА
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSlot(slot: SlotEntity)

    // Старый метод для удаления одиночного пустого слота (нужен для MainActivity)
    @Query("DELETE FROM slots WHERE id = :slotId")
    suspend fun deleteSlotById(slotId: String)

    // Новый метод для каскадного удаления веток папок и чатов (нужен для навигации)
    @Query("DELETE FROM slots WHERE id = :slotId OR id LIKE :slotId || '.%'")
    suspend fun deleteSlotAndChildren(slotId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(slots: List<SlotEntity>)

    @Query("UPDATE slots SET isMuted = :muted WHERE id = :slotId")
    suspend fun setMuted(slotId: String, muted: Boolean)

    @Query("UPDATE slots SET hasActivePing = :hasPing WHERE id = :slotId")
    suspend fun setHasActivePing(slotId: String, hasPing: Boolean)
}

// ПОВЫСИЛИ ВЕРСИЮ ДО 8 (чтобы старая база безопасно затерлась)
@Database(entities = [SlotEntity::class, MessageEntity::class, NodeEntity::class], version = 10)
abstract class AppDatabase : RoomDatabase() {
    abstract fun slotDao(): SlotDao
    abstract fun messageDao(): MessageDao
    abstract fun nodeDao(): NodeDao // Наш новый "кладовщик" для папок навигации
}