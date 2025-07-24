package com.example.intent_generator.annotation_function_builder

import com.example.intent_generator.IntentParam
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

class CompanionBuilder {

    operator fun invoke(
        pkg: String,
        intentClassName: String,
        nonNullableParams: List<IntentParam>,
        resultCodeValue: Int,
        resolveDefaultValue: (type: TypeName, name: String) -> String
    ): TypeSpec {
        return TypeSpec.companionObjectBuilder()
            .addFunction(
                FunSpec.builder("default")
                    .addParameter("activity", ClassName("android.app", "Activity"))
                    .returns(ClassName(pkg, intentClassName))
                    .addCode(
                        CodeBlock.builder().apply {
                            add("return %T(\n", ClassName(pkg, intentClassName))
                            add("    WeakReference(activity),\n")
                            add("    hasResultCode = false,\n")
                            add("    resultCode = %L,\n", resultCodeValue)
                            add("    animate = false,\n")
                            add("    finish = false,\n")
                            add("    clearTop = false,\n")
                            add("    newTask = false,\n")
                            // Add default values for non-nullable params
                            nonNullableParams.forEach { (name, type, _, defaultValue) ->
                                val param =
                                    if (type == STRING) "\"${defaultValue}\"" else defaultValue

                                val defaultValue = resolveDefaultValue(type, param)

                                add("    %L = %L,\n", name, defaultValue)
                            }
                            add(")\n")
                        }.build()
                    )
                    .build()
            )
            .build()
    }
}