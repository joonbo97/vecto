package com.vecto_example.vecto.ui.editfeed.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vecto_example.vecto.R

class MyEditImageAdapter (private val context: Context): RecyclerView.Adapter<MyEditImageAdapter.ViewHolder>(){
    val imageUri = mutableListOf<Uri>()
    val imageUrl = mutableListOf<String>()
    lateinit var recyclerView: RecyclerView

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view){
        val imageView: ImageView = view.findViewById(R.id.ImageItem)
        val deleteButton: ImageView = view.findViewById(R.id.DeleteButton)

    }
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.image_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return imageUri.size + imageUrl.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.imageView.clipToOutline = true

        if(position < imageUrl.size)//서버에서 전송받은 URL 세팅
        {
            Glide.with(context)
                .load(imageUrl[position])
                .placeholder(R.drawable.img_error_01) // 로딩 중 표시될 이미지
                .error(R.drawable.img_error_01) // 에러 발생 시 표시될 이미지
                .into(holder.imageView)
        }
        else
        {
            holder.imageView.setImageURI(imageUri[position - imageUrl.size])
        }



        holder.deleteButton.setOnClickListener {
            removeItem(position)
        }
    }

    private fun removeItem(position: Int) {
        if (position >= 0 && position < imageUri.size + imageUrl.size) {
            val itemView = recyclerView.findViewHolderForAdapterPosition(position)?.itemView
            val scaleDown = AnimationUtils.loadAnimation(context, R.anim.scale_down)

            scaleDown.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    // Nothing to do here
                }

                override fun onAnimationEnd(animation: Animation?) {
                    // 애니메이션이 완료되면 아이템 제거
                    if(position < imageUrl.size)//서버에서 전송받은 URL 세팅
                    {
                        imageUrl.removeAt(position)
                    }
                    else
                    {
                        imageUri.removeAt(position - imageUrl.size)
                    }

                    notifyDataSetChanged()
                    onItemRemovedListener?.onItemRemoved()
                }

                override fun onAnimationRepeat(animation: Animation?) {
                    // Nothing to do here
                }
            })

            itemView?.startAnimation(scaleDown)
        }
    }



    interface OnItemRemovedListener {
        fun onItemRemoved()
    }

    private var onItemRemovedListener: OnItemRemovedListener? = null

    fun setOnItemRemovedListener(listener: OnItemRemovedListener) {
        this.onItemRemovedListener = listener
    }

}