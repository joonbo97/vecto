package com.vecto_example.vecto.data.model

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import java.time.LocalDate
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

    fun deleteLocationDataBetween(startDatetime: String, endDatetime: String) {
        val db = dbHelper.writableDatabase
        val whereClause = "datetime > ? AND datetime < ?"
        val whereArgs = arrayOf(startDatetime, endDatetime)
        db.delete("location_data", whereClause, whereArgs)
        db.close()
    }

    @SuppressLint("Range")
    fun getBetweenLocationData(startDatetime: String, endDatetime: String): MutableList<LocationData> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM location_data WHERE datetime BETWEEN ? AND ? ORDER BY datetime ASC", arrayOf(startDatetime, endDatetime))
        val dataList = mutableListOf<LocationData>()

        while (cursor.moveToNext()) {
            val datetime = cursor.getString(cursor.getColumnIndex("datetime"))
            val lat = cursor.getDouble(cursor.getColumnIndex("lat"))
            val lng = cursor.getDouble(cursor.getColumnIndex("lng"))
            dataList.add(LocationData(datetime, lat, lng))
        }

        cursor.close()
        db.close()

        return dataList
    }

    @SuppressLint("Range")
    fun getTodayLocationData(): MutableList<LocationData> {
        val db = dbHelper.readableDatabase

        //YYYY-MM-DD 오늘 날짜
        val todayDate = LocalDate.now().toString()

        val startDatetime = "${todayDate}T00:00:00"
        val endDatetime = "${todayDate}T23:59:59"

        val cursor = db.rawQuery("SELECT * FROM location_data WHERE datetime BETWEEN ? AND ? ORDER BY datetime ASC", arrayOf(startDatetime, endDatetime))
        val dataList = mutableListOf<LocationData>()

        while (cursor.moveToNext()) {
            val datetime = cursor.getString(cursor.getColumnIndex("datetime"))
            val lat = cursor.getDouble(cursor.getColumnIndex("lat"))
            val lng = cursor.getDouble(cursor.getColumnIndex("lng"))
            dataList.add(LocationData(datetime, lat, lng))
        }

        cursor.close()
        db.close()

        return dataList
    }

    /**
     * 특정 DateTime에 해당하는 LocationData가 존재하는지 확인하는 함수
     * @param datetime: String
     * @return 존재 여부를 Boolean 값으로 반환
     */
    fun checkLocationDataExists(datetime: String): Boolean {
        val db = dbHelper.readableDatabase
        val projection = arrayOf("datetime")
        val selection = "datetime = ?"
        val selectionArgs = arrayOf(datetime)
        val cursor = db.query("location_data", projection, selection, selectionArgs, null, null, null)
        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }
}