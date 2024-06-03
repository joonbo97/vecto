package com.vecto_example.vecto.data.model

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.vecto_example.vecto.utils.VersionManager

class LocationDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_QUERY)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(DROP_TABLE_QUERY)
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "location_database"
        private const val DATABASE_VERSION = VersionManager.DATABASE_VERSION_CODE

        private const val CREATE_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS location_data (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "datetime TEXT, " +
                "lat REAL, " +
                "lng REAL " +
                ")"

        private const val DROP_TABLE_QUERY = "DROP TABLE IF EXISTS location_data"
    }
}
