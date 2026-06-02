package com.example.ordo

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NodeDao {
    // Получаем живой поток всех сот для автообновления карты
    @Query("SELECT * FROM nodes ORDER BY id ASC")
    fun getAllNodesFlow(): Flow<List<NodeEntity>>

    // Чтение всех сот списком для стартового построения
    @Query("SELECT * FROM nodes ORDER BY id ASC")
    suspend fun getAllNodes(): List<NodeEntity>

    // Сохранить или обновить узел соты
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: NodeEntity)

    // Рекурсивное удаление папки/чата и всего её содержимого на любой глубине за один запрос
    @Query("DELETE FROM nodes WHERE id = :nodeId OR id LIKE :nodeId || '.%'")
    suspend fun deleteNodeAndChildren(nodeId: String)

    // Быстрое переименование папки или чата
    @Query("UPDATE nodes SET name = :newName WHERE id = :nodeId")
    suspend fun updateNodeName(nodeId: String, newName: String)
}