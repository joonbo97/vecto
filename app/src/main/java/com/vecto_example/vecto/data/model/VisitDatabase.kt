package com.vecto_example.vecto.data.model

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import java.time.LocalDate

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
            put("address", visitData.address)
            put("distance", visitData.distance)
            put("type", visitData.transportType)
        }
        db.insert("visit_data", null, values)
        db.close()
    }

    @SuppressLint("Range")
    fun getAllVisitData(): MutableList<VisitData> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM visit_data ORDER BY datetime ASC", null)
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
            val address = cursor.getString(cursor.getColumnIndex("address"))
            val distance = cursor.getInt(cursor.getColumnIndex("distance"))
            val type = cursor.getString(cursor.getColumnIndex("type"))
            dataList.add(VisitData(datetime, endtime, lat, lng, lat_set, lng_set, staytime, name, address, distance, type))
        }

        cursor.close()
        db.close()

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
        val cursor = dbHelper.writableDatabase.rawQuery("SELECT * FROM visit_data ORDER BY datetime DESC LIMIT 1", null)

        cursor.moveToFirst()
        val datetime = cursor.getString(cursor.getColumnIndex("datetime"))
        val endtime = cursor.getString(cursor.getColumnIndex("endtime"))
        val lat = cursor.getDouble(cursor.getColumnIndex("lat"))
        val lng = cursor.getDouble(cursor.getColumnIndex("lng"))
        val lat_set = cursor.getDouble(cursor.getColumnIndex("lat_set"))
        val lng_set = cursor.getDouble(cursor.getColumnIndex("lng_set"))
        val staytime = cursor.getInt(cursor.getColumnIndex("staytime"))
        val name = cursor.getString(cursor.getColumnIndex("name"))
        val address = cursor.getString(cursor.getColumnIndex("address"))
        val distance = cursor.getInt(cursor.getColumnIndex("distance"))
        val type = cursor.getString(cursor.getColumnIndex("type"))
        cursor.close()

        return VisitData(datetime, endtime, lat, lng, lat_set, lng_set, staytime, name, address, distance, type)
    }

    //특정 시간의 데이터를 변경하는 작업
    fun updateVisitDataEndTime(datetime: String, endtime: String, staytime: Int) {
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

    fun updateVisitDataDistance(datetime: String, distance: Int) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("distance", distance)
        }
        val whereClause = "datetime = ?"
        val whereArgs = arrayOf(datetime)
        db.update("visit_data", values, whereClause, whereArgs)
        db.close()
    }

    fun updateVisitDataType(datetime: String, type: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("type", type)
        }
        val whereClause = "datetime = ?"
        val whereArgs = arrayOf(datetime)
        db.update("visit_data", values, whereClause, whereArgs)
        db.close()
    }

    @SuppressLint("Range")
    fun updateVisitDataAddress(newVisitData: VisitData) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("name", newVisitData.name)
            put("address", newVisitData.address)
        }

        val whereClause = "datetime = ?"
        val whereArgs = arrayOf(newVisitData.datetime)

        db.update("visit_data", values, whereClause, whereArgs) // 데이터 갱신
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
            put("address", newVisitData.address)
            put("distance", newVisitData.distance)
            put("type", newVisitData.transportType)
        }
        val whereClause = "datetime = ?" // 조건을 설정하여 갱신할 데이터 선택
        val whereArgs = arrayOf(oldVisitData.datetime) // 조건에 사용할 값들

        val locationdb = LocationDatabaseHelper(context).writableDatabase
        val locationValues = ContentValues().apply {
            put("datetime", newVisitData.datetime)
            put("lat", newVisitData.lat_set) // VisitData의 lat_set을 사용
            put("lng", newVisitData.lng_set) // VisitData의 lng_set을 사용
        }
        locationdb.update("location_data", locationValues, whereClause, whereArgs) // 데이터 갱신
        locationdb.close()

        db.update("visit_data", values, whereClause, whereArgs) // 데이터 갱신
        db.close()
    }

    @SuppressLint("Range")
    fun deleteVisitData(datetime: String) {
        val db = dbHelper.writableDatabase
        val whereClause = "datetime = ?"
        val whereArgs = arrayOf(datetime)
        db.delete("visit_data", whereClause, whereArgs)
        db.close()
    }

    @SuppressLint("Range")
    fun deleteVisitDataForEndtime(endtime: String) {
        val db = dbHelper.writableDatabase
        val whereClause = "endtime = ?"
        val whereArgs = arrayOf(endtime)
        db.delete("visit_data", whereClause, whereArgs)
        db.close()
    }

    @SuppressLint("Range")
    fun getTodayVisitData(): MutableList<VisitData> {
        val db = dbHelper.readableDatabase

        // 오늘 날짜를 YYYY-MM-DD 형식으로 가져옵니다.
        val todayDate = LocalDate.now().toString()

        val startDatetime = "${todayDate}T00:00:00"
        val endDatetime = "${todayDate}T23:59:59"

        val cursor = db.rawQuery("SELECT * FROM visit_data WHERE datetime BETWEEN ? AND ? ORDER BY datetime ASC", arrayOf(startDatetime, endDatetime))
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
            val address = cursor.getString(cursor.getColumnIndex("address"))
            val distance = cursor.getInt(cursor.getColumnIndex("distance"))
            val type = cursor.getString(cursor.getColumnIndex("type"))
            dataList.add(VisitData(datetime, endtime, lat, lng, lat_set, lng_set, staytime, name, address, distance, type))
        }

        cursor.close()
        db.close()

        return dataList
    }

    @SuppressLint("Range")
    fun getVisitDataByMonth(year: Int, month: Int): List<VisitData> {
        val db = dbHelper.readableDatabase

        val startDate = String.format("%d-%02d-01T00:00:00", year, month)
        val endDate = String.format("%d-%02d-31T23:59:59", year, month)

        val cursor = db.rawQuery("SELECT * FROM visit_data WHERE datetime BETWEEN ? AND ?", arrayOf(startDate, endDate))
        val dataList = mutableListOf<VisitData>()

        if (cursor.moveToFirst()) {
            do {
                val datetime = cursor.getString(cursor.getColumnIndex("datetime"))
                val endtime = cursor.getString(cursor.getColumnIndex("endtime"))
                val lat = cursor.getDouble(cursor.getColumnIndex("lat"))
                val lng = cursor.getDouble(cursor.getColumnIndex("lng"))
                val lat_set = cursor.getDouble(cursor.getColumnIndex("lat_set"))
                val lng_set = cursor.getDouble(cursor.getColumnIndex("lng_set"))
                val staytime = cursor.getInt(cursor.getColumnIndex("staytime"))
                val name = cursor.getString(cursor.getColumnIndex("name"))
                val address = cursor.getString(cursor.getColumnIndex("address"))
                val distance = cursor.getInt(cursor.getColumnIndex("distance"))
                val type = cursor.getString(cursor.getColumnIndex("type"))
                dataList.add(VisitData(datetime, endtime, lat, lng, lat_set, lng_set, staytime, name, address, distance, type))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return dataList
    }

}