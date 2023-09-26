package com.example.vecto.Data

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context

class VisitDatabase(context: Context) {
    private val dbHelper = VisitDatabaseHelper(context)

    fun addVisitData(visitData: VisitData) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("datetime", visitData.datetime)
            put("lat", visitData.lat)
            put("lng", visitData.lng)
            put("staytime", visitData.staytime)
        }
        db.insert("visit_data", null, values)
        db.close()
    }

    @SuppressLint("Range")
    fun getAllVisitData(): MutableList<VisitData> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM visit_data", null)
        val dataList = mutableListOf<VisitData>()

        while (cursor.moveToNext()) {
            val datetime = cursor.getString(cursor.getColumnIndex("datetime"))
            val lat = cursor.getDouble(cursor.getColumnIndex("lat"))
            val lng = cursor.getDouble(cursor.getColumnIndex("lng"))
            val staytime = cursor.getInt(cursor.getColumnIndex("staytime"))
            dataList.add(VisitData(datetime, lat, lng, staytime))
        }

        cursor.close()
        //db.close()

        return dataList
    }

    fun isVisitDatabaseEmpty(): Boolean {
        val cursor = dbHelper.readableDatabase.rawQuery("SELECT 1 FROM visit_data LIMIT 1", null)
        val isEmpty = cursor.count == 0
        cursor.close()
        return isEmpty
    }

    @SuppressLint("Range")
    fun getLastVisitData(): VisitData {
        val cursor = dbHelper.readableDatabase.rawQuery("SELECT * FROM visit_data ORDER BY id DESC LIMIT 1", null)

        cursor.moveToFirst()
        val datetime = cursor.getString(cursor.getColumnIndex("datetime"))
        val lat = cursor.getDouble(cursor.getColumnIndex("lat"))
        val lng = cursor.getDouble(cursor.getColumnIndex("lng"))
        val staytime = cursor.getInt(cursor.getColumnIndex("staytime"))
        cursor.close()

        return VisitData(datetime, lat, lng, staytime)
    }

}