package com.vecto_example.vecto.service

object Actions {
    /*  foreground service 를 위한 prefix  */

    private const val prefix = "com.example.vecto.action."
    const val MAIN = prefix + "main"
    const val START_FOREGROUND = prefix + "startforeground"
    const val STOP_FOREGROUND = prefix + "stopforeground"
}