package com.example.vecto.data

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context

class VisitDatabase(private val context: Context) {
    private val dbHelper = VisitDatabaseHelper(context)

    fun addVisitData(visitData: VisitData) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("datetime", visitData.datetime)
            put("endtime", visitData.endtime)
            put("lat", visitData.lat)
            put("lng", visitData.lng)
            put("lat_set", visitData.lat_set)
            put("lng_set", visitData.lng_set)
            put("staytime", visitData.staytime)
            put("name", visitData.name)
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
            val endtime = cursor.getString(cursor.getColumnIndex("endtime"))
            val lat = cursor.getDouble(cursor.getColumnIndex("lat"))
            val lng = cursor.getDouble(cursor.getColumnIndex("lng"))
            val lat_set = cursor.getDouble(cursor.getColumnIndex("lat_set"))
            val lng_set = cursor.getDouble(cursor.getColumnIndex("lng_set"))
            val staytime = cursor.getInt(cursor.getColumnIndex("staytime"))
            val name = cursor.getString(cursor.getColumnIndex("name"))
            dataList.add(VisitData(datetime, endtime, lat, lng, lat_set, lng_set, staytime, name))
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
        val cursor = dbHelper.writableDatabase.rawQuery("SELECT * FROM visit_data ORDER BY id DESC LIMIT 1", null)

        cursor.moveToFirst()
        val datetime = cursor.getString(cursor.getColumnIndex("datetime"))
        val endtime = cursor.getString(cursor.getColumnIndex("endtime"))
        val lat = cursor.getDouble(cursor.getColumnIndex("lat"))
        val lng = cursor.getDouble(cursor.getColumnIndex("lng"))
        val lat_set = cursor.getDouble(cursor.getColumnIndex("lat_set"))
        val lng_set = cursor.getDouble(cursor.getColumnIndex("lng_set"))
        val staytime = cursor.getInt(cursor.getColumnIndex("staytime"))
        val name = cursor.getString(cursor.getColumnIndex("name"))
        cursor.close()

        return VisitData(datetime, endtime, lat, lng, lat_set, lng_set, staytime, name)
    }

    //특정 시간의 데이터를 변경하는 작업
    fun updateVisitEndtimeData(datetime: String, endtime: String, staytime: Int) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("endtime", endtime)
            put("staytime", staytime)
        }
        val whereClause = "datetime = ?"
        val whereArgs = arrayOf(datetime)
        db.update("visit_data", values, whereClause, whereArgs)
        db.close()
    }

    @SuppressLint("Range")
    fun updateVisitData(oldVisitData: VisitData, newVisitData: VisitData) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("datetime", newVisitData.datetime)
            put("endtime", newVisitData.endtime)
            put("lat", newVisitData.lat)
            put("lng", newVisitData.lng)
            put("lat_set", newVisitData.lat_set)
            put("lng_set", newVisitData.lng_set)
            put("staytime", newVisitData.staytime)
            put("name", newVisitData.name)
        }
        val whereClause = "datetime = ?" // 조건을 설정하여 갱신할 데이터 선택
        val whereArgs = arrayOf(oldVisitData.datetime, oldVisitData.endtime) // 조건에 사용할 값들

        val locationdb = LocationDatabaseHelper(context).writableDatabase
        val locationValues = ContentValues().apply {
            put("datetime", newVisitData.datetime)
            put("lat", newVisitData.lat_set) // VisitData의 lat_set을 사용
            put("lng", newVisitData.lng_set) // VisitData의 lng_set을 사용
        }
        locationdb.update("location_data", locationValues, whereClause, whereArgs) // 데이터 갱신

        db.update("visit_data", values, whereClause, whereArgs) // 데이터 갱신
        db.close()
    }

}