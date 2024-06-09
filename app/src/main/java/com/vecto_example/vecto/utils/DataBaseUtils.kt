package com.vecto_example.vecto.utils

import com.vecto_example.vecto.data.model.LocationData
import com.vecto_example.vecto.data.model.LocationDatabase
import com.vecto_example.vecto.data.model.VisitData

class DataBaseUtils {
    companion object {

        /**
         * 방문지 간 이동 경로 분할을 위한 유효성 검증 함수.
         *
         * @param visitDataList 경로를 보여주기 위한 방문지 목록
         * @param locationDataList 선택된 모든 방문지들 사이의 이동 경로
         * @param locationDatabase 경로 데이터베이스
         */
        fun checkLocationDataValidation(visitDataList: MutableList<VisitData>, locationDataList: MutableList<LocationData>, locationDatabase: LocationDatabase) {
            var changeFlag = false

            for (visitData in visitDataList){
                if(!locationDatabase.checkLocationDataExists(visitData.datetime)){  //Visit 과 동일한 시간의 Location 이 없을 경우
                    locationDatabase.addLocationData(LocationData(visitData.datetime, visitData.lat, visitData.lng))
                    changeFlag = true
                }
            }

            if(changeFlag){
                locationDataList.clear()
                locationDataList.addAll(locationDatabase.getBetweenLocationData(visitDataList.first().datetime, visitDataList.last().datetime))
            }
        }
    }
}