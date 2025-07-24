package com.example.intent_generator

import com.squareup.kotlinpoet.TypeName

data class IntentParam(
    val name: String,
    val type: TypeName,
    val isNullable: Boolean,
    val defaultValue: String
)
