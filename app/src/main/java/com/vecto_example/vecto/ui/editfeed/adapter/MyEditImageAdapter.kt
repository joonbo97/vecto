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
import com.vecto_example.vecto.utils.LoadImageUtils

class MyEditImageAdapter (private val context: Context): RecyclerView.Adapter<MyEditImageAdapter.ViewHolder>(){
    val imageUri = mutableListOf<Uri>()
    val imageUrl = mutableListOf<String>()
    lateinit var recyclerView: RecyclerView

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view){
        val imageView: ImageView = view.findViewById(R.id.ImageItem)
        val deleteButton: ImageView = view.findViewById(R.id.DeleteButton)

        fun bindUri(uri: Uri) {
            imageView.clipToOutline = true

            imageView.setImageURI(imageUri[adapterPosition - imageUrl.size])


            deleteButton.setOnClickListener {
                removeItem(adapterPosition)
            }
        }

        fun bindUrl(url: String) {
            imageView.clipToOutline = true

            LoadImageUtils.loadImage(context, imageView, imageUrl[adapterPosition])

            deleteButton.setOnClickListener {
                removeItem(adapterPosition)
            }
        }
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
        if(position > imageUrl.lastIndex) {
            holder.bindUri(imageUri[position])
        } else {
            holder.bindUrl(imageUrl[position])
        }
    }

    private fun removeItem(position: Int) {
        if (position >= 0 && position < imageUri.size + imageUrl.size) {
            val itemView = recyclerView.findViewHolderForAdapterPosition(position)?.itemView
            val scaleDown = AnimationUtils.loadAnimation(context, R.anim.scale_down)

            if(position < imageUrl.size)//서버에서 전송받은 URL 세팅
            {
                imageUrl.removeAt(position)
                notifyItemRemoved(position)
            }
            else
            {
                imageUri.removeAt(position - imageUrl.size)
                notifyItemRemoved(position - imageUrl.size)
            }

            onItemRemovedListener?.onItemRemoved()

            scaleDown.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {}

                override fun onAnimationRepeat(animation: Animation?) {}
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