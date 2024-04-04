package com.vecto_example.vecto.ui.notification.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.model.NotificationDataResult
import com.vecto_example.vecto.data.model.NotificationDatabase
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MyNotificationAdapter(private val context: Context): RecyclerView.Adapter<MyNotificationAdapter.ViewHolder>(){
    val notificationData = mutableListOf<NotificationDataResult>()
    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.notificationText)
        val timeText: TextView = view.findViewById(R.id.PostTimeText)
        val circle: ImageView = view.findViewById(R.id.notificationCircle)
        val box: ImageView = view.findViewById(R.id.notificationBox)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.text.text = notificationData[position].text

        fun timeSince(dateTimeStr: String): String {
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val dateTimeFromDB = LocalDateTime.parse(dateTimeStr, formatter)

            val now = LocalDateTime.now()
            val duration = Duration.between(dateTimeFromDB, now)

            val daysPassed = duration.toDays()
            val hoursPassed = duration.toHours() % 24
            val minutesPassed = duration.toMinutes() % 60

            return when {
                daysPassed > 0 -> "${daysPassed}일 전"
                hoursPassed > 0 -> "${hoursPassed}시간 전"
                minutesPassed > 0 -> "${minutesPassed}분 전"
                else -> "방금 전"
            }
        }

        holder.timeText.text = timeSince(notificationData[position].datetime)

        if(notificationData[position].showFlag == 0)//보여지지 않았다면
        {
            holder.circle.visibility = View.VISIBLE
        }
        else//보여졌다면
        {
            holder.circle.visibility = View.INVISIBLE
        }

        holder.itemView.setOnClickListener {
            if(notificationData[position].showFlag == 0)//확인 안했으면
            {
                notificationData[position] = notificationData[position].copy(showFlag = 1)
                NotificationDatabase(context).updateShowFlag(notificationData[position].id)

                notifyItemChanged(position)
            }
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.notification_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return notificationData.size
    }
}