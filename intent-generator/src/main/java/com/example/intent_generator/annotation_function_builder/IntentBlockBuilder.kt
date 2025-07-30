package com.example.intent_generator.annotation_function_builder

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.CHAR_SEQUENCE
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.ksp.toTypeName
import kotlin.collections.contains

class IntentBlockBuilder(private val logger: KSPLogger) {

    operator fun invoke(targetName: String, params: List<KSAnnotation>): FunSpec {
        return FunSpec.getterBuilder()
            .addCode(
                CodeBlock.builder().apply {
                    add(
                        "return activity.get()?.let { \n Intent(it, %L::class.java).apply { \n",
                        targetName
                    )

                    for (annotation in params) {
                        val name =
                            annotation.arguments.first { it.name?.asString() == "name" }.value as String
                        val ksType =
                            annotation.arguments.first { it.name?.asString() == "type" }.value as KSType
                        val ksTypeArg =
                            annotation.arguments.first { it.name?.asString() == "typeArg" }.value as KSType
                        val isNullable =
                            annotation.arguments.firstOrNull { it.name?.asString() == "isNullable" }?.value as? Boolean == true

                        val kotlinType = ksType.toTypeName().copy(nullable = isNullable)

                        logger.info("buildIntentBlock $name $kotlinType")

                        fun returnKSTypeOfArray(): String {
                            val qualifiedName = ksTypeArg.declaration.qualifiedName?.asString()
                            val kotlinType = ksTypeArg.toTypeName().copy(nullable = isNullable)

                            logger.info("🧪 Checking array type: qualifiedName=$qualifiedName, kotlinType=$kotlinType")

                            return when (kotlinType) {
                                INT, INT.copy(nullable = true) -> {
                                    "putIntegerArrayListExtra"
                                }

                                STRING, STRING.copy(nullable = true) -> {
                                    "putStringArrayListExtra"
                                }

                                CHAR_SEQUENCE, CHAR_SEQUENCE.copy(
                                    nullable = true
                                ) -> {
                                    "putCharSequenceArrayListExtra"
                                }

                                else -> {
                                    logger.warn("⚠️ Defaulting to putParcelableArrayListExtra for $qualifiedName")
                                    "putParcelableArrayListExtra"
                                }
                            }.also {
                                logger.info("✅ Selected putExtra method: $it")
                            }
                        }

                        val putExtraMethod = when {
                            ksType.declaration.qualifiedName?.asString() in listOf(
                                "java.util.ArrayList",
                                "kotlin.collections.ArrayList"
                            ) -> returnKSTypeOfArray()

                            kotlinType == STRING -> "putExtra"
                            kotlinType == BOOLEAN -> "putExtra"
                            kotlinType == INT -> "putExtra"
                            kotlinType == LONG -> "putExtra"
                            kotlinType == FLOAT -> "putExtra"
                            kotlinType == DOUBLE -> "putExtra"
                            kotlinType == BYTE -> "putExtra" // no putByteExtra in Intent
                            kotlinType == CHAR -> "putExtra"
                            kotlinType == SHORT -> "putExtra"
                            else -> "putExtra"
                        }

                        if (isNullable) {
                            add("%L?.let { %L(%S, it) }\n", name, putExtraMethod, name)
                        } else {
                            add("%L(%S, %L)\n", putExtraMethod, name, name)
                        }
                    }

                    add("} \n } ?: throw com.example.intentgenerationsample.ActivityReferenceEmptyException()\n")
                }.build()
            )
            .build()
    }
}