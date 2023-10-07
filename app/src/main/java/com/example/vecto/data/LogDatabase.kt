package com.example.vecto.data

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import java.time.LocalDateTime

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