package com.vecto_example.vecto.utils

enum class FeedDetailType(val code:String) {

    INTENT_NORMAL("NORMAL"),        //일반
    INTENT_QUERY("QUERY"),          //검색
    INTENT_LIKE("LIKE"),            //좋아요 한 게시글
    INTENT_USERINFO("USERINFO")    //특정 사용자 게시글
}