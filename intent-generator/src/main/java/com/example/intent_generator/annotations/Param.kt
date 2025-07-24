package com.example.intent_generator.annotations

import kotlin.reflect.KClass

@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.CLASS,
    AnnotationTarget.ANNOTATION_CLASS
)
@Retention(AnnotationRetention.SOURCE)
annotation class Param(
    val name: String,
    val type: KClass<*>,
    val typeArg: KClass<*> = Unit::class,
    val isNullable: Boolean = true,
    val defaultValue: String = ""
)
