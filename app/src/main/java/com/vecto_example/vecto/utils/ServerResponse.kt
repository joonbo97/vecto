package com.vecto_example.vecto.utils

enum class ServerResponse(val code:String) {
    /*   SUCCESS   */

    SUCCESS_POSTFOLLOW("S024"),             //팔로우 등록 성공
    SUCCESS_DELETEFOLLOW("S025"),           //팔로우 해제 성공
    SUCCESS_ALREADY_POSTFOLLOW("S026"),     //팔로우 이미 등록
    SUCCESS_ALREADY_DELETEFOLLOW("S027"),   //팔로우 이미 해제
    SUCCESS_GETFOLLOW_FOLLOWING("S028"),    //팔로우 상태
    SUCCESS_GETFOLLOW_UNFOLLOWING("S029"),  //언팔로우 상태



    /*   ERROR   */
    FAIL_DUPLICATED_USERID("E018"),         //ID 중복
    FAIL_DUPLICATED_EMAIL("E019"),          //메일 중복
    FAIL_EMAILCODE_INVALID("E017"),         //인증 코드 오류

    /*   VISIT   */
    VISIT_TYPE_WALK("walk"),
    VISIT_TYPE_CAR("car"),
    VISIT_TYPE_PUBLIC("public"),
}