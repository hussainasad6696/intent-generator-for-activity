package com.example.intent_generator.annotation_function_builder

import com.example.intent_generator.IntentParam
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.STRING

class SecondaryConstructorBuilder {

    operator fun invoke(
        modifiedStandardProps: List<IntentParam>,
        nonNullableParams: List<IntentParam>,
        nullableParams: List<IntentParam>
    ): FunSpec {
        val secondaryCtor = FunSpec.constructorBuilder()
        (modifiedStandardProps + nonNullableParams + nullableParams)
            .forEach { (name, type, _, default) ->
                val paramBuilder = ParameterSpec.builder(name, type)
                // Only set default value if not "activity"
                if (name != "activity" && nonNullableParams.map { it.name }
                        .contains(name).not()) {

                    val param = if (type == STRING || type.toString()
                            .startsWith("kotlin.String") || type.toString()
                            .startsWith("kotlin.String?")
                    ) "\"${default}\"" else default
                    val defaultType = param.takeIf { it.isNotEmpty() }

                    val defaultValue = when {
                        type.isNullable -> "null"
                        type.toString()
                            .startsWith("kotlin.String") -> "\"${defaultType}\""
                            ?: "\"\""

                        type.toString()
                            .startsWith("kotlin.Boolean") -> runCatching {
                            defaultType?.toBoolean()?.toString()
                        }.getOrNull() ?: "false"

                        type.toString()
                            .startsWith("kotlin.Int") -> runCatching {
                            defaultType?.toInt()?.toString()
                        }.getOrNull() ?: "0"

                        type.toString()
                            .startsWith("kotlin.Long") -> runCatching {
                            defaultType?.toLong()?.toString()
                        }.getOrNull() ?: "0L"

                        type.toString()
                            .startsWith("kotlin.Float") -> runCatching {
                            defaultType?.toFloat()?.toString()
                        }.getOrNull() ?: "0f"

                        type.toString()
                            .startsWith("kotlin.Double") -> runCatching {
                            defaultType?.toDouble()?.toString()
                        }.getOrNull() ?: "0.0"

                        type.toString()
                            .startsWith("kotlin.Short") -> runCatching {
                            defaultType?.toShort()?.toString()
                        }.getOrNull() ?: "0"

                        type.toString()
                            .startsWith("kotlin.Byte") -> runCatching {
                            defaultType?.toByte()?.toString()
                        }.getOrNull() ?: "0"

                        type.toString()
                            .startsWith("kotlin.Char") -> runCatching {
                            defaultType?.toCharArray()?.getOrNull(0)?.toString()
                        }.getOrNull() ?: "'\\u0000'"

                        type is ParameterizedTypeName && type.rawType.simpleName == "ArrayList" -> "arrayListOf()"
                        else -> "null"
                    }
                    paramBuilder.defaultValue(defaultValue)
                }
                secondaryCtor.addParameter(paramBuilder.build())
            }
        val codeBlock = CodeBlock.builder()
        nullableParams.forEach { (name, _) ->
            codeBlock.addStatement("this.%L = %L", name, name)
        }

        return secondaryCtor
            .callThisConstructor(*(modifiedStandardProps + nonNullableParams).map { it.name }
                .toTypedArray())
            .addCode(codeBlock.build())
            .build()
    }
}