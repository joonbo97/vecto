package com.vecto_example.vecto.utils

enum class ServerResponse(val code:String) {
    /*   SUCCESS   */

    SUCCESS_POSTFOLLOW("S027"),             //팔로우 등록 성공
    SUCCESS_DELETEFOLLOW("S028"),           //팔로우 해제 성공
    SUCCESS_ALREADY_POSTFOLLOW("S029"),     //팔로우 이미 등록
    SUCCESS_ALREADY_DELETEFOLLOW("S030"),   //팔로우 이미 해제
    SUCCESS_GETFOLLOW_FOLLOWING("S028"),    //팔로우 상태
    SUCCESS_GETFOLLOW_UNFOLLOWING("S029"),  //언팔로우 상태



    /*   ERROR   */
    FAIL("FAIL"),
    ERROR("ERROR"),
    FAIL_DUPLICATED_USERID("E018"),         //ID 중복
    FAIL_DUPLICATED_EMAIL("E019"),          //메일 중복
    FAIL_EMAILCODE_INVALID("E017"),         //인증 코드 오류
    FAIL_GET_USERINFO("E020"),              //사용자 정보 조회 실패

    //TOKEN
    ACCESS_TOKEN_NOT_MATCH_ERROR("E021"),           //Access 토큰에 해당하는 사용자 정보가 없습니다
    ACCESS_TOKEN_INVALID_ERROR("E022"),             //Access 토큰이 잘못되었거나 유효기간이 만료되었습니다.
    ACCESS_TOKEN_IS_NULL_ERROR("E023"),             //Access 토큰이 존재하지 않습니다.
    ACCESS_REFRESH_TOKEN_IS_NULL_ERROR("E024"),     //Access 또는 Refresh 토큰이 없습니다
    ACCESS_TOKEN_VALID_ERROR("E025"),               //Access 토큰이 아직 유효합니다.
    REFRESH_TOKEN_INVALID_ERROR("E026"),            //Refresh 토큰이 유효하지 않습니다. 다시 로그인을 해주세요.
    REFRESH_TOKEN_NOT_EXIST_ERROR("E027"),          //Refresh 토큰이 DB에 저장되어 있지 않습니다.

    /*   VISIT   */
    VISIT_TYPE_WALK("walk"),
    VISIT_TYPE_CAR("car"),
    VISIT_TYPE_PUBLIC_TRANSPORT("public_transport"),

    /*   Report   */
    REPORT_TYPE_BAD_MANNER("BAD_MANNER"),
    REPORT_TYPE_INSULT("INSULT"),
    REPORT_TYPE_SEXUAL_HARASSMENT("SEXUAL_HARASSMENT"),
    REPORT_TYPE_OTHER_PROBLEM("OTHER_PROBLEM"),
}