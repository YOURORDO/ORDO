package com.example.ordo

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val msgId: String, // msgId теперь главный ключ. Никаких дубликатов в БД физически быть не может!
    val slotId: String,
    val text: String,
    val status: String,
    val timestamp: Long,
    val isMine: Boolean
)

@Dao
interface MessageDao {
    // --- ПРОВЕРКА НА ДУБЛИКАТЫ ---
    @Query("SELECT COUNT(*) FROM messages WHERE msgId = :mId")
    suspend fun checkExists(mId: String): Int

    @Query("UPDATE messages SET text = :newText WHERE msgId = :mId")
    suspend fun updateText(mId: String, newText: String)

    // Разрешаем выгружать сообщения со стандартными статусами ИЛИ со статусами, начинающимися на "DELIVERED:"
    @Query("SELECT * FROM messages WHERE slotId = :slotId AND (status IN ('SENDING', 'DELIVERED', 'FAILED') OR status LIKE 'DELIVERED:%') ORDER BY timestamp DESC")
    fun getBridgeMessages(slotId: String): Flow<List<MessageEntity>>

    // В ЧЕРНОВИК (Shadow) попадает ТОЛЬКО черновик, готовое к отправке или ожидающее
    @Query("SELECT * FROM messages WHERE slotId = :slotId AND status IN ('EDITING', 'READY', 'PENDING') ORDER BY timestamp DESC")
    fun getShadowMessages(slotId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE slotId = :slotId AND status = 'READY' AND isMine = 1")
    suspend fun getReadyMessages(slotId: String): List<MessageEntity>

    @Query("UPDATE messages SET status = :newStatus WHERE msgId = :mId")
    suspend fun updateStatus(mId: String, newStatus: String)

    // --- ВОТ ЭТОТ МЕТОД НУЖНО БЫЛО ДОБАВИТЬ ---
    @Query("UPDATE messages SET status = :newStatus WHERE slotId = :slotId AND status = :oldStatus")
    suspend fun updateStatusByStatus(slotId: String, oldStatus: String, newStatus: String)

    @Query("UPDATE messages SET status = 'FAILED' WHERE msgId = :mId AND status = 'SENDING'")
    suspend fun rollbackStatus(mId: String)

    @Query("UPDATE messages SET status = 'FAILED' WHERE status = 'SENDING'")
    suspend fun resetStuckMessages()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Delete
    suspend fun delete(message: MessageEntity)

    @Query("UPDATE messages SET status = 'PENDING' WHERE slotId = :slotId AND (status = 'READY' OR status = 'SENDING')")
    suspend fun markAsPending(slotId: String)

    // Рекурсивная очистка всех сообщений для удаляемой ветки папок/чатов
    @Query("DELETE FROM messages WHERE slotId = :slotId OR slotId LIKE :slotId || '.%'")
    suspend fun deleteMessagesAndChildren(slotId: String)
}