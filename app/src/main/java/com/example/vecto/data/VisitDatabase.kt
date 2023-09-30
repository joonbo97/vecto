package com.example.vecto.data

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context

class VisitDatabase(context: Context) {
    private val dbHelper = VisitDatabaseHelper(context)

    fun addVisitData(visitData: VisitData) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("datetime", visitData.datetime)
            put("endtime", visitData.endtime)
            put("lat", visitData.lat)
            put("lng", visitData.lng)
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
            val staytime = cursor.getInt(cursor.getColumnIndex("staytime"))
            val name = cursor.getString(cursor.getColumnIndex("name"))
            dataList.add(VisitData(datetime, endtime, lat, lng, staytime, name))
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
        val staytime = cursor.getInt(cursor.getColumnIndex("staytime"))
        val name = cursor.getString(cursor.getColumnIndex("name"))
        cursor.close()

        return VisitData(datetime, endtime, lat, lng, staytime, name)
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
    fun getVisitDateData(date: String): MutableList<VisitData> {
        val db = dbHelper.writableDatabase
        val cursor = db.rawQuery("SELECT * FROM visit_data WHERE datetime LIKE ?", arrayOf("$date%"))

        val dataList = mutableListOf<VisitData>()

        if (cursor.moveToFirst()) {
            val datetime = cursor.getString(cursor.getColumnIndex("datetime"))
            val endtime = cursor.getString(cursor.getColumnIndex("endtime"))
            val lat = cursor.getDouble(cursor.getColumnIndex("lat"))
            val lng = cursor.getDouble(cursor.getColumnIndex("lng"))
            val staytime = cursor.getInt(cursor.getColumnIndex("staytime"))
            val name = cursor.getString(cursor.getColumnIndex("name"))
            dataList.add(VisitData(datetime, endtime, lat, lng, staytime, name))
        }

        cursor.close()
        return dataList
    }
}