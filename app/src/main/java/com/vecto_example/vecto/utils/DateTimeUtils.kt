package com.vecto_example.vecto.utils

import java.text.SimpleDateFormat
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
    }
}