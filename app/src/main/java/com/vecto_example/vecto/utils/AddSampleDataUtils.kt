package com.vecto_example.vecto.utils

import android.content.Context
import com.vecto_example.vecto.data.model.LocationData
import com.vecto_example.vecto.data.model.LocationDatabase
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.data.model.VisitDatabase

class AddSampleDataUtils {
    companion object{
        fun addData(context: Context){
            /*  sample data */
            VisitDatabase(context).addVisitData(VisitData("2023-11-08T10:00:00", "2023-11-08T11:00:00",
                37.5112474, 127.098459, 37.5112474,127.098459, 60, ""))
            LocationDatabase(context).addLocationData(LocationData("2023-11-08T10:00:00", 37.5112474, 127.098459))

            VisitDatabase(context).addVisitData(VisitData("2023-11-08T11:00:00", "2023-11-08T13:00:00",
                37.5126404, 127.102612, 37.5126404,127.102612, 120, ""))
            LocationDatabase(context).addLocationData(LocationData("2023-11-08T11:00:00", 37.5126404, 127.102612))

            VisitDatabase(context).addVisitData(VisitData("2023-11-08T17:00:00", "2023-11-08T19:00:00",
                37.5145638, 127.108768, 37.5145638,127.108768, 120, ""))
            LocationDatabase(context).addLocationData(LocationData("2023-11-08T17:00:00", 37.5145638, 127.108768))
        }
    }
}