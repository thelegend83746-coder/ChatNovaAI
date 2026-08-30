package com.chatnova.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chatnova.ai.data.local.entity.ModelCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelCacheDao {
    @Query("SELECT * FROM models_cache ORDER BY isFree DESC, name ASC")
    fun getAllModels(): Flow<List<ModelCacheEntity>>

    @Query("SELECT * FROM models_cache WHERE id = :id LIMIT 1")
    suspend fun getModelById(id: String): ModelCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<ModelCacheEntity>)

    @Query("DELETE FROM models_cache")
    suspend fun clearModels()
}
