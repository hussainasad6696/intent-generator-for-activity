package com.example.intent_generator.annotation_function_builder

import com.example.intent_generator.IntentParam
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName

class PrimaryConstructorBuilder(private val pkg: String, private val intentClassName: String) {

    operator fun FileSpec.Builder.invoke(
        modifiedStandardProps: List<IntentParam>,
        nonNullableParams: List<IntentParam>,
        nullableParams: List<IntentParam>,
        resultCodeValue: Int,
        companionBuilder: CompanionBuilder,
        resolveDefaultValue: (type: TypeName, param: String) -> String
    ): FunSpec.Builder {
        val primaryCtor = FunSpec.constructorBuilder()

        (modifiedStandardProps + nonNullableParams).forEach { intentParam ->
            val paramBuilder =
                ParameterSpec.builder(intentParam.name, intentParam.type)

            val param =
                if (intentParam.type == STRING) "\"${intentParam.defaultValue}\"" else intentParam.defaultValue
            val resolvedDefault = resolveDefaultValue(
                intentParam.type,
                param
            )

            if (intentParam.name != "activity" && intentParam.name != "resultCode")
                paramBuilder.defaultValue(resolvedDefault)

            if (intentParam.name == "resultCode")
                paramBuilder.defaultValue("%L", resultCodeValue)

            primaryCtor.addParameter(paramBuilder.build())
            addProperty(
                PropertySpec.builder(intentParam.name, intentParam.type)
                    .initializer(intentParam.name)
                    .apply {
                        if (modifiedStandardProps.any { it.name == intentParam.name }) addModifiers(
                            KModifier.OVERRIDE
                        )
                        else mutable(true)
                    }
                    .build()
            )
        }

        addType(
            companionBuilder.invoke(
                nonNullableParams = nonNullableParams,
                resultCodeValue = resultCodeValue,
                pkg = pkg,
                intentClassName = intentClassName,
                resolveDefaultValue = resolveDefaultValue
            )
        )

        // Add nullable properties (default null)
        nullableParams.forEach { (name, type, _, defaultValue) ->
            val param = if (type == STRING || type.toString()
                    .startsWith("kotlin.String") || type.toString()
                    .startsWith("kotlin.String?")
            ) "\"${defaultValue}\"" else defaultValue

            addProperty(
                PropertySpec.builder(name, type)
                    .initializer(param.takeIf { it.isNotEmpty() } ?: "null")
                    .mutable(true)
                    .build()
            )
        }

        return primaryCtor
    }
}