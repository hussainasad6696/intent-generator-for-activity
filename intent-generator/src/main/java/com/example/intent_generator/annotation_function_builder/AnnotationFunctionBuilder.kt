package com.example.intent_generator.annotation_function_builder

import com.example.intent_generator.IntentParam
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName

class AnnotationFunctionBuilder(
    private val logger: KSPLogger,
    val pkg: String,
    val className: String
) {

    fun resolveDefaultValue(type: TypeName, default: String, from: String): String {
        logger.warn("Present in resolveDefaultValue type $type == default $default from $from")
        if (default.isNotEmpty() and default.isNotBlank()) return if (type == STRING || type == STRING.copy(nullable = true))
            "\"$default\""
        else default
        return when {
            type.isNullable -> "null"
            type == STRING || type == STRING.copy(nullable = true) -> "\"\""
            type == BOOLEAN || type == BOOLEAN.copy(nullable = true) -> "false"
            type == INT || type == INT.copy(nullable = true) -> "0"
            type == LONG || type == LONG.copy(nullable = true) -> "0L"
            type == FLOAT || type == FLOAT.copy(nullable = true) -> "0f"
            type == DOUBLE || type == DOUBLE.copy(nullable = true) -> "0.0"
            type == SHORT || type == SHORT.copy(nullable = true) -> "0"
            type == BYTE || type == BYTE.copy(nullable = true) -> "0"
            type == CHAR || type == CHAR.copy(nullable = true) -> "'\\u0000'"
            type is ParameterizedTypeName && type.rawType.simpleName == "ArrayList" -> "arrayListOf()"
            else -> "null"
        }
    }

    fun companionBuilder(
        nonNullableParams: List<IntentParam>,
        resultCodeValue: Int
    ) = CompanionBuilder().invoke(
        pkg = pkg,
        intentClassName = className,
        nonNullableParams = nonNullableParams,
        resultCodeValue = resultCodeValue,
        resolveDefaultValue = ::resolveDefaultValue
    )

    fun getDataHandlerBuilder(
        resultCodeValue: Int,
        standardProps: List<IntentParam>,
        nonNullableParams: List<IntentParam>,
        nullableParams: List<IntentParam>,
        paramAnnotations: List<KSAnnotation>
    ) = GetDataHandlerBuilder().invoke(
        intentClassName = className,
        resultCodeValue = resultCodeValue,
        standardProps = standardProps,
        nonNullableParams = nonNullableParams,
        nullableParams = nullableParams,
        paramAnnotations = paramAnnotations
    )

    fun intentBlockBuilder(
        targetName: String,
        paramAnnotations: List<KSAnnotation>
    ) = IntentBlockBuilder(logger).invoke(
        targetName = targetName,
        params = paramAnnotations
    )

    fun FileSpec.Builder.primaryConstructor(
        modifiedStandardProps: List<IntentParam>,
        nonNullableParams: List<IntentParam>,
        nullableParams: List<IntentParam>,
        resultCodeValue: Int
    ) = with(PrimaryConstructorBuilder(pkg, className)) {
        invoke(
            modifiedStandardProps = modifiedStandardProps,
            nonNullableParams = nonNullableParams,
            nullableParams = nullableParams,
            companionBuilder = CompanionBuilder(),
            resultCodeValue = resultCodeValue,
            resolveDefaultValue = ::resolveDefaultValue
        )
    }

//    fun secondaryConstructorBuilder(
//        modifiedStandardProps: List<IntentParam>,
//        nonNullableParams: List<IntentParam>,
//        nullableParams: List<IntentParam>
//    ) = SecondaryConstructorBuilder().invoke(
//        modifiedStandardProps = modifiedStandardProps,
//        nonNullableParams = nonNullableParams,
//        nullableParams = nullableParams,
//        resolveDefaultValue = ::resolveDefaultValue
//    )
}