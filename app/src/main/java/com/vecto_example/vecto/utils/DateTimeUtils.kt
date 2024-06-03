package com.vecto_example.vecto.utils

import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class DateTimeUtils {
    companion object {
        fun getPreviousDate(selectedDate: String): String {
            val selectedDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val selectedDateObj = selectedDateFormat.parse(selectedDate)
            val calendar = Calendar.getInstance()
            calendar.time = selectedDateObj!!
            calendar.add(Calendar.DAY_OF_MONTH, -1)

            return selectedDateFormat.format(calendar.time)
        }

        fun getCourseTime(datetime1: String, datetime2: String): String {
            val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

            val date1 = LocalDateTime.parse(datetime1, format)
            val date2 = LocalDateTime.parse(datetime2, format)

            val minutesPassed = Duration.between(date1, date2).toMinutes().toInt()

            return if(minutesPassed < 60) {
                "약 1시간 이내 코스"
            } else{
                "약 ${minutesPassed/60}시간 코스"
            }
        }

        fun isValidDateTimeFormat(date: String): Boolean {
            return try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                dateFormat.isLenient = false
                dateFormat.parse(date)
                true
            } catch (e: Exception) {
                false
            }
        }

        fun getNoticeTime(inputDate: String): String? {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            return try {
                val date = inputFormat.parse(inputDate)
                date?.let { outputFormat.format(it) }
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }

        fun isNewNotice(inputDate: String): Boolean {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

            return try {
                val date = inputFormat.parse(inputDate)
                date?.let {
                    val today = Calendar.getInstance()
                    val threeDaysAgo = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -3)  // 과거 3일 전으로 설정
                    }
                    // 날짜가 오늘보다 이전이면서 과거 3일 전 이후인지 확인
                    it.before(today.time) && it.after(threeDaysAgo.time)
                } ?: false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}