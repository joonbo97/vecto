package com.vecto_example.vecto.dialog

import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.model.VisitDatabase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

class CalendarDialog (private val context: Context) {//deletepostdialog copy
    private val dialog = Dialog(context, R.style.CustomDialog)
    var onOkButtonClickListener: (() -> Unit)? = null
    var onNoButtonClickListener: (() -> Unit)? = null
    var onDateSelectedListener: OnDateSelectedListener? = null


    var selectedDate = ""
    var lastSelect = -1

    private val dateTextViews = arrayOf(R.id.DateText01, R.id.DateText02, R.id.DateText03, R.id.DateText04, R.id.DateText05, R.id.DateText06, R.id.DateText07, R.id.DateText08, R.id.DateText09, R.id.DateText10, R.id.DateText11, R.id.DateText12,R.id.DateText13,R.id.DateText14, R.id.DateText15, R.id.DateText16, R.id.DateText17, R.id.DateText18,R.id.DateText19, R.id.DateText20, R.id.DateText21, R.id.DateText22, R.id.DateText23, R.id.DateText24, R.id.DateText25, R.id.DateText26, R.id.DateText27, R.id.DateText28, R.id.DateText29, R.id.DateText30, R.id.DateText31,R.id.DateText32, R.id.DateText33, R.id.DateText34, R.id.DateText35, R.id.DateText36, R.id.DateText37, R.id.DateText38, R.id.DateText39, R.id.DateText40, R.id.DateText41, R.id.DateText42)

    var year = 0
    var month = 0

    interface OnDateSelectedListener {
        fun onDateSelected(date: String)
    }

    fun showDialog() {
        dialog.setContentView(R.layout.dialog_calendar)
        dialog.window!!.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)

        val currentCalendar = Calendar.getInstance()
        year = currentCalendar.get(Calendar.YEAR)
        month = currentCalendar.get(Calendar.MONTH)
        initCalendar(year, month)

        Log.d("YEAR MONTH3", "$year $month")

        dialog.show()

        val OKButton: TextView = dialog.findViewById(R.id.OKText)
        val NOButton: TextView = dialog.findViewById(R.id.NOText)

        OKButton.setOnClickListener {
            if (selectedDate.isNotEmpty()) {
                onOkButtonClickListener?.invoke()
                selectedDate.let { date ->
                    onDateSelectedListener?.onDateSelected(date)
                }
                dialog.dismiss()
            }
            else{
                Toast.makeText(context, "선택된 날짜가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        NOButton.setOnClickListener{
            onNoButtonClickListener?.invoke()
            dialog.dismiss()
        }

        val BeforeButton: ImageView = dialog.findViewById(R.id.BeforeButton)
        val AfterButton: ImageView = dialog.findViewById(R.id.AfterButton)

        BeforeButton.setOnClickListener {


            if (month == 0)
            {
                year--
                month = 11
            }
            else
                month--

            initCalendar(year, month)
            selectedDate = ""
            lastSelect = -1
            dialog.findViewById<TextView>(R.id.OKText).setTextColor(ContextCompat.getColor(context, R.color.edit_gray))
        }

        AfterButton.setOnClickListener {


            if(month == 11)
            {
                year++
                month = 0
            }
            else
                month++

            initCalendar(year, month)
            selectedDate = ""
            lastSelect = -1
            dialog.findViewById<TextView>(R.id.OKText).setTextColor(ContextCompat.getColor(context, R.color.edit_gray))
        }


    }

    fun dismiss(){
        dialog.dismiss()
    }

    private fun initCalendar(year: Int, month: Int){
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        dialog.findViewById<TextView>(R.id.OKText).setTextColor(ContextCompat.getColor(context, R.color.edit_gray))

        dialog.findViewById<TextView>(R.id.DateText)?.text =
            String.format("%d년 %d월", year, month + 1)

        dateTextViews.forEach { id ->
            dialog.findViewById<TextView>(id).apply {
                text = ""
                visibility = View.INVISIBLE
            }
        }

        val visitDataList = VisitDatabase(context).getVisitDataByMonth(year, month + 1)
        val datesWithData = visitDataList.map { visitData ->
            LocalDateTime.parse(visitData.datetime, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate().dayOfMonth
        }.toHashSet()

        for (i in 1..daysInMonth) {
            dialog.findViewById<TextView>(dateTextViews[firstDayOfWeek + i - 2]).apply {
                text = i.toString()
                visibility = View.VISIBLE

                if(i in datesWithData){

                    this.apply{
                        setTextColor(ContextCompat.getColor(context, R.color.black))
                        setOnClickListener {
                            selectedDate = String.format("%d-%02d-%02d", year, month + 1, i)
                            setTextColor(ContextCompat.getColor(context, R.color.vecto_theme_orange))

                            dialog.findViewById<TextView>(R.id.OKText).setTextColor(ContextCompat.getColor(context, R.color.black))

                            if(lastSelect != -1 && lastSelect != firstDayOfWeek + i - 2)    //이전에 선택한 날짜가 있으면 이전 선택된 날짜 색 변경
                            {
                                dialog.findViewById<TextView>(dateTextViews[lastSelect]).setTextColor(ContextCompat.getColor(context, R.color.black))
                            }
                            lastSelect = firstDayOfWeek + i - 2
                        }
                    }

                }
                else{
                    this.apply {
                        setTextColor(ContextCompat.getColor(context, R.color.edit_gray))
                        setOnClickListener(null)
                    }

                }

            }
        }

    }
}