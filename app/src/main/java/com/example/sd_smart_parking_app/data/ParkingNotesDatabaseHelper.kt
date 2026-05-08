package com.example.sd_smart_parking_app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.sd_smart_parking_app.data.model.ParkingNote

class ParkingNotesDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "parking_notes.db"
        const val DATABASE_VERSION = 1
        const val TABLE_NAME = "parking_notes"
        const val COLUMN_ID = "id"
        const val COLUMN_USER_ID = "user_id"
        const val COLUMN_USER_EMAIL = "user_email"
        const val COLUMN_USER_NAME = "user_name"
        const val COLUMN_MESSAGE = "message"
        const val COLUMN_FLOOR = "floor"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_IS_SYNCED = "is_synced"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_USER_ID TEXT NOT NULL,
                $COLUMN_USER_EMAIL TEXT NOT NULL,
                $COLUMN_USER_NAME TEXT NOT NULL,
                $COLUMN_MESSAGE TEXT NOT NULL,
                $COLUMN_FLOOR INTEGER DEFAULT 0,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_IS_SYNCED INTEGER DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createTable)
        Log.d("ParkingNotesDB", "Table created: $TABLE_NAME")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertNote(note: ParkingNote, isSynced: Boolean = false): Boolean {
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_ID, note.id)
                put(COLUMN_USER_ID, note.userId)
                put(COLUMN_USER_EMAIL, note.userEmail)
                put(COLUMN_USER_NAME, note.userName)
                put(COLUMN_MESSAGE, note.message)
                put(COLUMN_FLOOR, note.floor)
                put(COLUMN_TIMESTAMP, note.timestamp)
                put(COLUMN_IS_SYNCED, if (isSynced) 1 else 0)
            }
            val result = db.insertWithOnConflict(
                TABLE_NAME,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
            Log.d("ParkingNotesDB", "Note inserted: ${note.id}, synced: $isSynced")
            result != -1L
        } catch (e: Exception) {
            Log.e("ParkingNotesDB", "Error inserting note: ${e.message}", e)
            false
        }
    }

    fun getLastTenNotes(): List<ParkingNote> {
        val notes = mutableListOf<ParkingNote>()
        return try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                "$COLUMN_TIMESTAMP DESC",
                "10"
            )
            cursor.use {
                while (it.moveToNext()) {
                    notes.add(
                        ParkingNote(
                            id = it.getString(it.getColumnIndexOrThrow(COLUMN_ID)),
                            userId = it.getString(it.getColumnIndexOrThrow(COLUMN_USER_ID)),
                            userEmail = it.getString(it.getColumnIndexOrThrow(COLUMN_USER_EMAIL)),
                            userName = it.getString(it.getColumnIndexOrThrow(COLUMN_USER_NAME)),
                            message = it.getString(it.getColumnIndexOrThrow(COLUMN_MESSAGE)),
                            floor = it.getInt(it.getColumnIndexOrThrow(COLUMN_FLOOR)),
                            timestamp = it.getLong(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
                            isLocal = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_SYNCED)) == 0
                        )
                    )
                }
            }
            Log.d("ParkingNotesDB", "Retrieved ${notes.size} notes from SQLite")
            notes
        } catch (e: Exception) {
            Log.e("ParkingNotesDB", "Error getting notes: ${e.message}", e)
            emptyList()
        }
    }

    fun getUnsyncedNotes(): List<ParkingNote> {
        val notes = mutableListOf<ParkingNote>()
        return try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_NAME,
                null,
                "$COLUMN_IS_SYNCED = 0",
                null,
                null,
                null,
                "$COLUMN_TIMESTAMP DESC"
            )
            cursor.use {
                while (it.moveToNext()) {
                    notes.add(
                        ParkingNote(
                            id = it.getString(it.getColumnIndexOrThrow(COLUMN_ID)),
                            userId = it.getString(it.getColumnIndexOrThrow(COLUMN_USER_ID)),
                            userEmail = it.getString(it.getColumnIndexOrThrow(COLUMN_USER_EMAIL)),
                            userName = it.getString(it.getColumnIndexOrThrow(COLUMN_USER_NAME)),
                            message = it.getString(it.getColumnIndexOrThrow(COLUMN_MESSAGE)),
                            floor = it.getInt(it.getColumnIndexOrThrow(COLUMN_FLOOR)),
                            timestamp = it.getLong(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
                            isLocal = true
                        )
                    )
                }
            }
            Log.d("ParkingNotesDB", "Retrieved ${notes.size} unsynced notes")
            notes
        } catch (e: Exception) {
            Log.e("ParkingNotesDB", "Error getting unsynced notes: ${e.message}", e)
            emptyList()
        }
    }

    fun markNoteAsSynced(noteId: String): Boolean {
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_IS_SYNCED, 1)
            }
            val rows = db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(noteId))
            Log.d("ParkingNotesDB", "Note marked as synced: $noteId")
            rows > 0
        } catch (e: Exception) {
            Log.e("ParkingNotesDB", "Error marking note as synced: ${e.message}", e)
            false
        }
    }

    fun getNoteCount(): Int {
        return try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_NAME", null)
            cursor.use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
        } catch (e: Exception) {
            0
        }
    }
}