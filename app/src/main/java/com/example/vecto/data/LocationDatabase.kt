package com.example.vecto.data

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import java.time.LocalDateTime

class LocationDatabase(context: Context) {
    private val dbHelper = LocationDatabaseHelper(context)

    fun addLocationData(locationData: LocationData) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("datetime", locationData.datetime)
            put("lat", locationData.lat)
            put("lng", locationData.lng)
        }
        db.insert("location_data", null, values)
        db.close()
    }

    @SuppressLint("Range")
    fun getAllLocationData(): MutableList<LocationData> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM location_data ORDER BY datetime ASC", null)
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

    //일정 시간 이후의 데이터를 지우는 작업
    fun deleteLocationDataAfter(datetime: LocalDateTime) {
        val db = dbHelper.writableDatabase
        val whereClause = "datetime >= ?"
        val whereArgs = arrayOf(datetime.toString())
        db.delete("location_data", whereClause, whereArgs)
        db.close()
    }

    //특정 시간의 데이터를 변경하는 작업
    fun updateLocationData(datetime: String, newLat: Double, newLng: Double) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("lat", newLat)
            put("lng", newLng)
        }
        val whereClause = "datetime = ?"
        val whereArgs = arrayOf(datetime)
        db.update("location_data", values, whereClause, whereArgs)
        db.close()
    }

    @SuppressLint("Range")
    fun getLocationDateData(date: String): List<LocationData> {
        val db = dbHelper.writableDatabase
        val cursor = db.rawQuery("SELECT * FROM location_data WHERE datetime LIKE ? ORDER BY datetime ASC", arrayOf("$date%"))

        val dataList = mutableListOf<LocationData>()

        while (cursor.moveToNext()) {
            val datetime = cursor.getString(cursor.getColumnIndex("datetime"))
            val lat = cursor.getDouble(cursor.getColumnIndex("lat"))
            val lng = cursor.getDouble(cursor.getColumnIndex("lng"))
            val locationData = LocationData(datetime, lat, lng)
            dataList.add(locationData)
        }

        cursor.close()
        return dataList
    }

    fun deleteLocationDataBetween(startDatetime: String, endDatetime: String) {
        val db = dbHelper.writableDatabase
        val whereClause = "datetime > ? AND datetime < ?"
        val whereArgs = arrayOf(startDatetime, endDatetime)
        db.delete("location_data", whereClause, whereArgs)
        db.close()
    }
}