package com.chatnova.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chatnova.ai.domain.model.AiModel

@Entity(tableName = "models_cache")
data class ModelCacheEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val contextLength: Int,
    val promptPrice: Double,
    val completionPrice: Double,
    val isFree: Boolean,
    val hasVision: Boolean,
    val provider: String
) {
    fun toDomain() = AiModel(
        id = id,
        name = name,
        description = description,
        contextLength = contextLength,
        promptPrice = promptPrice,
        completionPrice = completionPrice,
        isFree = isFree,
        hasVision = hasVision,
        provider = provider
    )

    companion object {
        fun fromDomain(model: AiModel) = ModelCacheEntity(
            id = model.id,
            name = model.name,
            description = model.description,
            contextLength = model.contextLength,
            promptPrice = model.promptPrice,
            completionPrice = model.completionPrice,
            isFree = model.isFree,
            hasVision = model.hasVision,
            provider = model.provider
        )
    }
}
