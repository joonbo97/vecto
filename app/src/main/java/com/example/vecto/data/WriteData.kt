package com.example.vecto.data

data class WriteData(
    val title: String, //제목
    val content: String, //내용
    val uploadtime: String, //게시 시간
    val image: MutableList<String>, //이미지
    val location: MutableList<VisitData>, //경로 정보
    val visit: MutableList<VisitData> //방문지 정보
)