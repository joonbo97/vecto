package com.example.vecto.data

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase

class NotificationDatabase (context: Context){
    private val dbHelper = NotificationDatabaseHelper(context)

    fun addNotificationData(notificationData: NotificationData){
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("datetime", notificationData.datetime)
            put("feedID", notificationData.feedId)
            put("text", notificationData.text)
            put("showFlag", notificationData.showFlag)
        }
        db.insert("notification_data", null, values)
        db.close()
    }

    @SuppressLint("Range")
    fun getAllNotificationData(): MutableList<NotificationDataResult>{
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM notification_data ORDER BY id DESC", null)
        val notifications = mutableListOf<NotificationDataResult>()

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndex("id"))
            val datetime = cursor.getString(cursor.getColumnIndex("datetime"))
            val feedID = cursor.getInt(cursor.getColumnIndex("feedID"))
            val text = cursor.getString(cursor.getColumnIndex("text"))
            val showFlag = cursor.getInt(cursor.getColumnIndex("showFlag"))
            notifications.add(NotificationDataResult(id, datetime, feedID, text, showFlag))
        }

        cursor.close()
        db.close()

        return notifications
    }

    fun updateShowFlag(id: Int) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("showFlag", 1)
        }
        db.update("notification_data", values, "id=?", arrayOf(id.toString()))
        db.close()
    }

    fun checkShowFlag(): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT 1 FROM notification_data WHERE showFlag = 0 LIMIT 1", null)
        val hasUnshown = cursor.moveToFirst()
        cursor.close()
        db.close()
        return hasUnshown
    }

    fun doesNotificationTableExist(): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='notification_data'",
            null
        )
        val count = cursor.use { it.moveToFirst(); it.getInt(0) }
        return count > 0
    }

}