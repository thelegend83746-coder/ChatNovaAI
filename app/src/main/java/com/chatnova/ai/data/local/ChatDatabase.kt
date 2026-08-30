package com.chatnova.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.chatnova.ai.data.local.dao.ConversationDao
import com.chatnova.ai.data.local.dao.MessageDao
import com.chatnova.ai.data.local.dao.ModelCacheDao
import com.chatnova.ai.data.local.entity.ConversationEntity
import com.chatnova.ai.data.local.entity.MessageEntity
import com.chatnova.ai.data.local.entity.ModelCacheEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        ModelCacheEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun modelCacheDao(): ModelCacheDao

    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        fun getInstance(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "chatnova_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
