package com.gotv.player.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.gotv.player.data.model.Channel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Lightweight, low-memory SQLite implementation of [ChannelRepository].
 *
 * Uses Android platform SQLiteOpenHelper with zero external runtime overhead,
 * maintaining an active in-memory StateFlow updated on write operations.
 */
class SqliteChannelRepository(
    context: Context,
    dbName: String? = DATABASE_NAME,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ChannelRepository {

    private val dbHelper = DatabaseHelper(context, dbName)
    private val _channels = MutableStateFlow<List<Channel>>(emptyList())

    init {
        // Initial synchronous load for the repository instance
        _channels.value = readAllFromDb()
    }

    override fun getAllChannels(): Flow<List<Channel>> = _channels.asStateFlow()

    override suspend fun getChannelById(id: String): Channel? = withContext(ioDispatcher) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            PROJECTION,
            "$COLUMN_ID = ?",
            arrayOf(id),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                mapRow(it)
            } else {
                null
            }
        }
    }

    override suspend fun addChannel(channel: Channel) = withContext(ioDispatcher) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("UPDATE $TABLE_NAME SET $COLUMN_POSITION = $COLUMN_POSITION + 1")
            val values = ContentValues().apply {
                put(COLUMN_ID, channel.id)
                put(COLUMN_NAME, channel.name)
                put(COLUMN_STREAM_URL, channel.streamUrl)
                put(COLUMN_LOGO_URL, channel.logoUrl)
                put(COLUMN_LOCAL_LOGO_PATH, channel.localLogoPath)
                put(COLUMN_CREATED_AT, channel.createdAt)
                put(COLUMN_POSITION, 0)
            }
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _channels.value = readAllFromDb()
    }

    override suspend fun updateChannel(channel: Channel) = withContext(ioDispatcher) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, channel.name)
            put(COLUMN_STREAM_URL, channel.streamUrl)
            put(COLUMN_LOGO_URL, channel.logoUrl)
            put(COLUMN_LOCAL_LOGO_PATH, channel.localLogoPath)
        }
        db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(channel.id))
        _channels.value = readAllFromDb()
    }

    override suspend fun deleteChannel(id: String) = withContext(ioDispatcher) {
        val db = dbHelper.writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id))
        _channels.value = readAllFromDb()
    }

    override suspend fun reorderChannels(orderedChannelIds: List<String>) = withContext(ioDispatcher) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            orderedChannelIds.forEachIndexed { index, id ->
                val values = ContentValues().apply {
                    put(COLUMN_POSITION, index)
                }
                db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(id))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _channels.value = readAllFromDb()
    }

    private fun readAllFromDb(): List<Channel> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            PROJECTION,
            null,
            null,
            null,
            null,
            "$COLUMN_POSITION ASC, $COLUMN_CREATED_AT DESC"
        )
        val channels = mutableListOf<Channel>()
        cursor.use {
            while (it.moveToNext()) {
                channels.add(mapRow(it))
            }
        }
        return channels
    }

    private fun mapRow(cursor: android.database.Cursor): Channel {
        val id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID))
        val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
        val streamUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STREAM_URL))
        val logoUrl = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_LOGO_URL))) null else cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOGO_URL))
        val localLogoPath = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_LOCAL_LOGO_PATH))) null else cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCAL_LOGO_PATH))
        val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT))
        val position = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_POSITION))

        return Channel(
            id = id,
            name = name,
            streamUrl = streamUrl,
            logoUrl = logoUrl,
            localLogoPath = localLogoPath,
            createdAt = createdAt,
            position = position
        )
    }

    private class DatabaseHelper(context: Context, name: String?) : SQLiteOpenHelper(context, name, null, DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                    $COLUMN_ID TEXT PRIMARY KEY,
                    $COLUMN_NAME TEXT NOT NULL,
                    $COLUMN_STREAM_URL TEXT NOT NULL,
                    $COLUMN_LOGO_URL TEXT,
                    $COLUMN_LOCAL_LOGO_PATH TEXT,
                    $COLUMN_CREATED_AT INTEGER NOT NULL,
                    $COLUMN_POSITION INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion < 2) {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_POSITION INTEGER NOT NULL DEFAULT 0")
                val cursor = db.query(
                    TABLE_NAME,
                    arrayOf(COLUMN_ID),
                    null,
                    null,
                    null,
                    null,
                    "$COLUMN_CREATED_AT DESC"
                )
                val ids = mutableListOf<String>()
                cursor.use {
                    while (it.moveToNext()) {
                        ids.add(it.getString(0))
                    }
                }
                ids.forEachIndexed { index, id ->
                    val values = ContentValues().apply {
                        put(COLUMN_POSITION, index)
                    }
                    db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(id))
                }
            }
        }
    }

    companion object {
        const val DATABASE_NAME = "gotv_channels.db"
        const val DATABASE_VERSION = 2
        const val TABLE_NAME = "channels"

        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_STREAM_URL = "stream_url"
        const val COLUMN_LOGO_URL = "logo_url"
        const val COLUMN_LOCAL_LOGO_PATH = "local_logo_path"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_POSITION = "position"

        private val PROJECTION = arrayOf(
            COLUMN_ID,
            COLUMN_NAME,
            COLUMN_STREAM_URL,
            COLUMN_LOGO_URL,
            COLUMN_LOCAL_LOGO_PATH,
            COLUMN_CREATED_AT,
            COLUMN_POSITION
        )
    }
}

