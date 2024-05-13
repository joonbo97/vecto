package com.vecto_example.vecto.ui.notification.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.ui.comment.CommentActivity
import com.vecto_example.vecto.R
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.onefeed.OneFeedActivity
import com.vecto_example.vecto.ui.userinfo.UserInfoActivity

class MyNotificationAdapter(private val context: Context): RecyclerView.Adapter<MyNotificationAdapter.ViewHolder>(){
    val notificationData = mutableListOf<VectoService.Notification>()
    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.notificationText)
        val timeText: TextView = view.findViewById(R.id.PostTimeText)
        val box: ImageView = view.findViewById(R.id.notificationBox)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.text.text = notificationData[position].content

        holder.timeText.text =notificationData[position].timeDifference

        if(!notificationData[position].requestedBefore)//보여지지 않았다면
        {
            holder.box.setBackgroundResource(R.drawable.ripple_highlight_box)
        }
        else//보여졌다면
        {
            holder.box.setBackgroundResource(R.drawable.ripple_normal_box)
        }

        holder.itemView.setOnClickListener {
            notificationData[position].requestedBefore = true
            notifyItemChanged(position)

            if(notificationData[position].notificationType == "follow"){
                val intent = Intent(context, UserInfoActivity::class.java)
                intent.putExtra("userId", notificationData[position].fromUserId)
                context.startActivity(intent)
            }
            else if(notificationData[position].notificationType == "comment"){
                val intent = Intent(context, OneFeedActivity::class.java)
                intent.putExtra("feedId", notificationData[position].feedId)
                intent.putExtra("isComment", true)

                context.startActivity(intent)
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

    fun addNotificationData(newData: List<VectoService.Notification>) {
        //데이터 추가 함수
        val startIdx = notificationData.size
        notificationData.addAll(newData)
        notifyItemRangeInserted(startIdx, newData.size)
    }
}