package com.vecto_example.vecto.utils

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.kakao.sdk.common.util.KakaoCustomTabsClient
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.share.WebSharerClient
import com.kakao.sdk.template.model.Button
import com.kakao.sdk.template.model.Content
import com.kakao.sdk.template.model.FeedTemplate
import com.kakao.sdk.template.model.ItemContent
import com.kakao.sdk.template.model.Link
import com.kakao.sdk.template.model.Social
import com.vecto_example.vecto.retrofit.VectoService

class ShareFeedUtil {
    companion object {
        fun shareFeed(context: Context, feedInfo: VectoService.FeedInfo){
            val defaultFeed = FeedTemplate(
                content = Content(
                    title = feedInfo.title,
                    description = "코스를 공유해요!",
                    imageUrl = feedInfo.mapImage[0],
                    link = Link(
                        webUrl = "https://developers.kakao.com",
                        mobileWebUrl = "https://developers.kakao.com"
                    )
                ),
                itemContent = ItemContent(
                    profileText = feedInfo.nickName,
                    profileImageUrl = feedInfo.userProfile
                ),
                social = Social(
                    likeCount = feedInfo.likeCount,
                    commentCount = feedInfo.commentCount
                ),
                buttons = listOf(
                    Button(
                        "앱으로 보기",
                        Link(
                            androidExecutionParams = mapOf("feedId" to feedInfo.feedId.toString())
                        )
                    )
                )
            )

            // 카카오톡 설치여부 확인
            if (ShareClient.instance.isKakaoTalkSharingAvailable(context)) {

                ShareClient.instance.shareDefault(context, defaultFeed) { sharingResult, error ->
                    if (error != null) {
                        Log.e(ContentValues.TAG, "카카오톡 공유 실패", error)
                    }
                    else if (sharingResult != null) {
                        Log.d(ContentValues.TAG, "카카오톡 공유 성공 ${sharingResult.intent}")
                        context.startActivity(sharingResult.intent)
                    }
                }
            } else {
                // 카카오톡 미설치
                val sharerUrl = WebSharerClient.instance.makeDefaultUrl(defaultFeed)

                try {
                    KakaoCustomTabsClient.openWithDefault(context, sharerUrl)
                } catch(e: UnsupportedOperationException) {
                    Toast.makeText(context, "인터넷 브라우저가 없습니다. 브라우저 설치 후 재시도 해주세요.", Toast.LENGTH_SHORT).show()
                }

                try {
                    KakaoCustomTabsClient.open(context, sharerUrl)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "인터넷 브라우저가 없습니다. 브라우저 설치 후 재시도 해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}