package com.vecto_example.vecto.utils

enum class ServerResponse(val code:String) {
    /*   SUCCESS   */

    SUCCESS_GETFOLLOW_FOLLOWING("S028"),    //팔로우 상태
    SUCCESS_GETFOLLOW_UNFOLLOWING("S029"),  //언팔로우 상태
    SUCCESS_POSTFOLLOW("S024"),             //팔로우 등록 성공
    SUCCESS_DELETEFOLLOW("S025"),           //팔로우 해제 성공
    SUCCESS_ALREADY_POSTFOLLOW("S026"),     //팔로우 이미 등록
    SUCCESS_ALREADY_DELETEFOLLOW("S027"),   //팔로우 이미 해제


    /*   ERROR   */


}