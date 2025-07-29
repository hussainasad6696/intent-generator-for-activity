package com.example.intent_generator.annotation_function_builder

import com.example.intent_generator.IntentParam
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName

class SecondaryConstructorBuilder {

    operator fun invoke(
        modifiedStandardProps: List<IntentParam>,
        nonNullableParams: List<IntentParam>,
        nullableParams: List<IntentParam>,
        resolveDefaultValue: (type: TypeName, param: String) -> String
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

                    val defaultValue = resolveDefaultValue(type, param)

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