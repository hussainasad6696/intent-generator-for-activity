package com.example.intent_generator.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GenerateIntent(
    val target: KClass<*>,
    val params: Array<Param> = []
)
