package com.example.intent_generator.annotation_function_builder

import com.example.intent_generator.IntentParam
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import java.util.logging.Logger

class SecondaryConstructorBuilder(private val logger: KSPLogger) {

    operator fun invoke(
        modifiedStandardProps: List<IntentParam>,
        nonNullableParams: List<IntentParam>,
        nullableParams: List<IntentParam>,
        resolveDefaultValue: (type: TypeName, param: String) -> String
    ): FunSpec {
        val secondaryCtor = FunSpec.constructorBuilder()
        (modifiedStandardProps + nonNullableParams + nullableParams)
            .forEach { (name, type, _, default) ->
                val defaultValue =
                    resolveDefaultValue(
                        type,
                        default,
                    )

                val newType = if (defaultValue.isEmpty() or (defaultValue == "null")) type
                else type.copy(nullable = false)

                val paramBuilder = ParameterSpec.builder(name, newType)
                // Only set default value if not "activity"


                logger.info("SecondaryConstructorBuilder: resolvedDefault $defaultValue")

                if (name != "activity" && nonNullableParams.map { it.name }
                        .contains(name).not())
                    paramBuilder.defaultValue(defaultValue)
                else if (default.isNotEmpty()) paramBuilder.defaultValue(
                    defaultValue
                )

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