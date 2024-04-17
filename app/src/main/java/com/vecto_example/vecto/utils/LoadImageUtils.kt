package com.vecto_example.vecto.utils

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth

class LoadImageUtils {

    companion object {
        fun loadProfileImage(context: Context, imageView: ImageView) {
            if (Auth._profileImage.value == null) {
                imageView.setImageResource(R.drawable.profile_basic)
            }
            else//사용자 정의 이미지가 있을 경우
            {
                Glide.with(context)
                    .load(Auth._profileImage.value)
                    .circleCrop()
                    .placeholder(R.drawable.profile_basic) // 로딩 중 표시될 이미지
                    .error(R.drawable.profile_basic) // 에러 발생 시 표시될 이미지
                    .into(imageView)
            }
        }

        fun loadUserProfileImage(context: Context, imageView: ImageView, profileUrl: String?) {
            if (profileUrl == null) {
                imageView.setImageResource(R.drawable.profile_basic)
            }
            else//사용자 정의 이미지가 있을 경우
            {
                Glide.with(context)
                    .load(profileUrl)
                    .circleCrop()
                    .placeholder(R.drawable.profile_basic) // 로딩 중 표시될 이미지
                    .error(R.drawable.profile_basic) // 에러 발생 시 표시될 이미지
                    .into(imageView)
            }
        }

        fun loadImage(context: Context, imageView: ImageView, imageUrl: String?) {

            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.img_error_01) // 로딩 중 표시될 이미지
                .error(R.drawable.img_error_01) // 에러 발생 시 표시될 이미지
                .into(imageView)
        }
    }
}