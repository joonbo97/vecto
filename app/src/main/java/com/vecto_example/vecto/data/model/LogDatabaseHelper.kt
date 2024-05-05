package com.vecto_example.vecto.data.model

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LogDatabaseHelper (context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_QUERY)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(DROP_TABLE_QUERY)
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "log_database"
        private const val DATABASE_VERSION = 6

        private const val CREATE_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS log_data (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "datetime TEXT, " +
                "log TEXT" +
                ")"

        private const val DROP_TABLE_QUERY = "DROP TABLE IF EXISTS log_data"
    }
}
