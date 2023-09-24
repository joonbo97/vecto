package com.example.vecto.Data

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context

class LocationDatabase(context: Context) {
    private val dbHelper = LocationDatabaseHelper(context)

    fun addLocationData(locationData: LocationData) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("date", locationData.date)
            put("time", locationData.time)
            put("lat", locationData.lat)
            put("lng", locationData.lng)
        }
        db.insert("location_data", null, values)
        db.close()
    }

    @SuppressLint("Range")
    fun getAllLocationData(): MutableList<LocationData> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM location_data", null)
        val dataList = mutableListOf<LocationData>()

        while (cursor.moveToNext()) {
            val date = cursor.getString(cursor.getColumnIndex("date"))
            val time = cursor.getString(cursor.getColumnIndex("time"))
            val lat = cursor.getDouble(cursor.getColumnIndex("lat"))
            val lng = cursor.getDouble(cursor.getColumnIndex("lng"))
            dataList.add(LocationData(date, time, lat, lng))
        }

        cursor.close()
        db.close()

        return dataList
    }
}