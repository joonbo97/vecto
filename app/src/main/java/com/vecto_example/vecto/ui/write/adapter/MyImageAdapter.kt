package com.vecto_example.vecto.ui.write.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.R


class MyImageAdapter (private val context: Context): RecyclerView.Adapter<MyImageAdapter.ViewHolder>(){
    val imageUri = mutableListOf<Uri>()
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
        return imageUri.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = imageUri[position]
        holder.imageView.clipToOutline = true
        holder.imageView.setImageURI(uri)

        holder.deleteButton.setOnClickListener {
            removeItem(position)
        }
    }

    private fun removeItem(position: Int) {
        if (position >= 0 && position < imageUri.size) {
            val itemView = recyclerView.findViewHolderForAdapterPosition(position)?.itemView
            val scaleDown = AnimationUtils.loadAnimation(context, R.anim.scale_down)

            scaleDown.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    // Nothing to do here
                }

                override fun onAnimationEnd(animation: Animation?) {
                    // 애니메이션이 완료되면 아이템 제거
                    imageUri.removeAt(position)
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