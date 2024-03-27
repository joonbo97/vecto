package com.vecto_example.vecto.data.model

import android.content.ContentValues
import android.content.Context

class LogDatabase(context: Context) {
    private val dbHelper = LogDatabaseHelper(context)

    fun addLogData(logData: LogData) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("datetime", logData.datetime)
            put("log", logData.log)
        }
        db.insert("log_data", null, values)
        //db.close()
    }
}