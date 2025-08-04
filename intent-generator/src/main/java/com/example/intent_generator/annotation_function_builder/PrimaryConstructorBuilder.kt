package com.example.intent_generator.annotation_function_builder

import com.example.intent_generator.IntentParam
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import java.util.logging.Logger
import kotlin.collections.forEach
import kotlin.collections.plus

class PrimaryConstructorBuilder(private val logger: KSPLogger) {

    operator fun invoke(
        modifiedStandardProps: List<IntentParam>,
        nonNullableParams: List<IntentParam>,
        nullableParams: List<IntentParam>,
        resultCodeValue: Int,
        typeSpecs: TypeSpec.Builder,
        resolveDefaultValue: (type: TypeName, param: String) -> String
    ): FunSpec {
        return FunSpec.constructorBuilder().apply {
            (modifiedStandardProps + nonNullableParams).forEach { intentParam ->
                val paramBuilder =
                    ParameterSpec.builder(intentParam.name, intentParam.type)

                val resolvedDefault = resolveDefaultValue(
                    intentParam.type,
                    intentParam.defaultValue
                )

                logger.info("PrimaryConstructorBuilder: resolvedDefault $resolvedDefault")

                if (intentParam.name != "activity" && intentParam.name != "resultCode")
                    paramBuilder.defaultValue(resolvedDefault)

                if (intentParam.name == "resultCode")
                    paramBuilder.defaultValue("%L", resultCodeValue)

                addParameter(paramBuilder.build())
                typeSpecs.addProperty(
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
        }.build()
    }
}