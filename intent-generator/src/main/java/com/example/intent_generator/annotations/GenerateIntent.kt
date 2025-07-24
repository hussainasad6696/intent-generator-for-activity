package com.example.intent_generator.annotations


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GenerateIntent(
    val resultCode: Int = 0,
    val params: Array<Param> = []
)
