package com.example.vecto.Data

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context

class VisitDatabase(context: Context) {
    private val dbHelper = VisitDatabaseHelper(context)

    fun addVisitData(locationData: LocationData) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("datetime", locationData.datetime)
            put("lat", locationData.lat)
            put("lng", locationData.lng)
        }
        db.insert("visit_data", null, values)
        db.close()
    }

    @SuppressLint("Range")
    fun getAllVisitData(): MutableList<LocationData> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM visit_data", null)
        val dataList = mutableListOf<LocationData>()

        while (cursor.moveToNext()) {
            val datetime = cursor.getString(cursor.getColumnIndex("datetime"))
            val lat = cursor.getDouble(cursor.getColumnIndex("lat"))
            val lng = cursor.getDouble(cursor.getColumnIndex("lng"))
            dataList.add(LocationData(datetime, lat, lng))
        }

        cursor.close()
        //db.close()

        return dataList
    }
}