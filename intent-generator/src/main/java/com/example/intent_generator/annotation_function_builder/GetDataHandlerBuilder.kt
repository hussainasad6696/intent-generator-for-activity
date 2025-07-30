package com.example.intent_generator.annotation_function_builder

import com.example.intent_generator.IntentParam
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.CHAR_SEQUENCE
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import kotlin.collections.contains

class GetDataHandlerBuilder(private val pkg: String, private val className: String) {

    operator fun invoke(
        resultCodeValue: Int,
        standardProps: List<IntentParam>,
        nonNullableParams: List<IntentParam>,
        nullableParams: List<IntentParam>,
        paramAnnotations: List<KSAnnotation>
    ): FunSpec {
        fun getReturnIntentType(name: String, type: TypeName): String {
            val annotation = paramAnnotations.firstOrNull { it.getString("name") == name }
            val ksType = annotation?.getKSTypeOrNull("type")
            val ksTypeArg = annotation?.getKSTypeOrNull("typeArg")
            val qualifiedNameForType = ksType?.declaration?.qualifiedName?.asString()
            val qualifiedNameForTypeArg = ksTypeArg?.declaration?.qualifiedName?.asString()
            val kotlinType = ksTypeArg?.toTypeName()

            return when {
                ksType?.declaration?.qualifiedName?.asString() in listOf(
                    "java.util.ArrayList", "kotlin.collections.ArrayList"
                ) -> {
                    when (kotlinType) {
                        INT, INT.copy(nullable = true) -> "getIntegerArrayListExtra"
                        STRING, STRING.copy(nullable = true) -> "getStringArrayListExtra"
                        CHAR_SEQUENCE, CHAR_SEQUENCE.copy(nullable = true) -> "getCharSequenceArrayListExtra"
                        else -> "getParcelableArrayListExtra<$qualifiedNameForTypeArg>"
                    }
                }

                type == STRING || type == STRING.copy(nullable = true) -> "getStringExtra"
                type == BOOLEAN || type == BOOLEAN.copy(nullable = true) -> "getBooleanExtra"
                type == INT || type == INT.copy(nullable = true) -> "getIntExtra"
                type == LONG || type == LONG.copy(nullable = true) -> "getLongExtra"
                type == FLOAT || type == FLOAT.copy(nullable = true) -> "getFloatExtra"
                type == DOUBLE || type == DOUBLE.copy(nullable = true) -> "getDoubleExtra"
                type == SHORT || type == SHORT.copy(nullable = true) -> "getShortExtra"
                type == BYTE || type == BYTE.copy(nullable = true) -> "getByteExtra"
                type == CHAR || type == CHAR.copy(nullable = true) -> "getCharExtra"
                else -> "getSerializableExtra"
            }
        }

        return FunSpec.builder("getDataHandler")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(
                "intent",
                ClassName("android.content", "Intent").copy(nullable = true)
            )
            .returns(
                ClassName(
                    pkg,
                    className
                )
            )
            .addCode(
                CodeBlock.builder().apply {
                    addStatement("val intent = intent ?: activity.get()?.intent")
                    add("return %L(\n", className)

                    val primaryParams = standardProps + nonNullableParams
                    primaryParams.forEachIndexed { i, intentParam ->
                        val method = getReturnIntentType(intentParam.name, intentParam.type)

                        val valueExpr = when {
                            intentParam.name == "activity" -> "activity"
                            intentParam.name == "resultCode" -> "$resultCodeValue"
                            intentParam.name == "animate" -> "false"
                            intentParam.name == "finish" -> "false"
                            intentParam.name == "clearTop" -> "false"
                            intentParam.name == "newTask" -> "false"
                            method.startsWith("getParcelableArrayListExtra") -> {
                                val generic = method.substringAfter("<").substringBefore(">")
                                "intent?.getParcelableArrayListExtra<$generic>(\"${intentParam.name}\") ?: arrayListOf()"
                            }

                            method == "getBooleanExtra" -> "intent?.$method(\"${intentParam.name}\", false) ?: false"
                            method == "getIntExtra" -> "intent?.$method(\"${intentParam.name}\", 0) ?: 0"
                            method == "getLongExtra" -> "intent?.$method(\"${intentParam.name}\", 0L) ?: 0L"
                            method == "getFloatExtra" -> "intent?.$method(\"${intentParam.name}\", 0f) ?: 0f"
                            method == "getDoubleExtra" -> "intent?.$method(\"${intentParam.name}\", 0.0) ?: 0.0"
                            method == "getShortExtra" -> "intent?.$method(\"${intentParam.name}\", 0.toShort()) ?: 0.toShort()"
                            method == "getStringExtra" -> "intent?.$method(\"${intentParam.name}\") ?: \"\""
                            method == "getByteExtra" -> "intent?.$method(\"${intentParam.name}\", 0.toByte()) ?: 0.toByte()"
                            method == "getCharExtra" -> "intent?.$method(\"${intentParam.name}\", '\\u0000') ?: '\\u0000'"
                            else -> "intent?.$method(\"${intentParam.name}\") as? ${intentParam.type}"
                        }

                        val comma = if (i < primaryParams.size - 1) "," else ""
                        addStatement("    %L = %L$comma", intentParam.name, valueExpr)
                    }

                    add(").apply {\n")

                    nullableParams.forEach { (name, type) ->
                        val method = getReturnIntentType(name, type)

                        val statement = when {
                            method.startsWith("getParcelableArrayListExtra") -> {
                                val generic = method.substringAfter("<").substringBefore(">")
                                "intent?.getParcelableArrayListExtra<$generic>(\"$name\")"
                            }

                            method == "getSerializableExtra" -> """
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                   intent?.getParcelableExtra("$name", ${type.copy(nullable = false)}::class.java)
                            } else {
                                    @Suppress("DEPRECATION")
                                    intent?.getParcelableExtra("$name")
                            }
                     """.trimIndent()

                            method == "getBooleanExtra" -> "intent?.getBooleanExtra(\"$name\", false)"
                            method == "getIntExtra" -> "intent?.getIntExtra(\"$name\", 0)"
                            method == "getLongExtra" -> "intent?.getLongExtra(\"$name\", 0L)"
                            method == "getFloatExtra" -> "intent?.getFloatExtra(\"$name\", 0f)"
                            method == "getDoubleExtra" -> "intent?.getDoubleExtra(\"$name\", 0.0)"
                            method == "getShortExtra" -> "intent?.getShortExtra(\"$name\", 0)"
                            method == "getByteExtra" -> "intent?.getByteExtra(\"$name\", 0)"
                            method == "getCharExtra" -> "intent?.getCharExtra(\"$name\", '\\u0000')"
                            method == "getStringExtra" -> "intent?.$method(\"$name\")"
                            else -> "intent?.$method(\"$name\")"
                        }

                        addStatement("    this.%L = %L", name, statement)
                    }

                    add("}\n")
                }.build()
            )
            .build()
    }

    private fun KSAnnotation.getString(key: String): String? =
        arguments.firstOrNull { it.name?.asString() == key }?.value as? String

//    private fun KSAnnotation.getBoolean(key: String): Boolean? =
//        arguments.firstOrNull { it.name?.asString() == key }?.value as? Boolean

    private fun KSAnnotation.getKSTypeOrNull(key: String): KSType? =
        arguments.firstOrNull { it.name?.asString() == key }?.value as? KSType
}