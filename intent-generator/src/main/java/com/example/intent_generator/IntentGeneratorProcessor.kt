package com.example.intent_generator

import com.example.intent_generator.annotations.GenerateIntent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.toTypeName
import java.io.OutputStreamWriter

//./gradlew assembleDebug
//@AutoService(SymbolProcessor::class)
class IntentProcessor(
    private val codeGen: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.warn("🛠️ IntentProcessor is running...")
        logger.info("Present in intent processor")
        resolver.getSymbolsWithAnnotation(GenerateIntent::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { classDecl ->
                logger.info("Present in intent processor forEach $classDecl")
                generateClass(classDecl)
            }
        return emptyList()
    }

    private fun generateClass(classDecl: KSClassDeclaration) {
        val annotation = classDecl.annotations.first { it.shortName.asString() == "GenerateIntent" }

        val targetType = annotation.arguments.first { it.name?.asString() == "target" }.value as KSType
        val paramsValue = annotation.arguments.firstOrNull { it.name?.asString() == "params" }?.value
        val paramAnnotations = paramsValue as? List<KSAnnotation> ?: emptyList()

        val targetName = targetType.declaration.simpleName.asString()
        val pkg = classDecl.packageName.asString()
        val intentClassName = "${targetName}Intent"

        val fileSpec = FileSpec.builder(pkg, intentClassName).apply {
            addType(TypeSpec.classBuilder(intentClassName).apply {
                superclass(ClassName("com.example.intentgenerationsample", "IntentHandler"))

                val activityType = ClassName("java.lang.ref", "WeakReference")
                    .parameterizedBy(ClassName("android.app", "Activity"))

                val standardProps = listOf(
                    "activity" to activityType,
                    "hasResultCode" to BOOLEAN,
                    "resultCode" to INT,
                    "animate" to BOOLEAN,
                    "finish" to BOOLEAN,
                    "clearTop" to BOOLEAN.copy(nullable = true)
                )

                val ctor = FunSpec.constructorBuilder()
                standardProps.forEach { (name, type) ->
                    ctor.addParameter(name, type)
                    addProperty(
                        PropertySpec.builder(name, type)
                            .initializer(name)
                            .addModifiers(KModifier.OVERRIDE)
                            .build()
                    )
                }

                for (paramAnnotation in paramAnnotations) {
                    var name = ""
                    var type: TypeName = UNIT
                    var isNullable = true

                    for (arg in paramAnnotation.arguments) {
                        when (arg.name?.asString()) {
                            "name" -> name = arg.value as String
                            "type" -> {
                                val ksType = arg.value as KSType
                                type = ksType.toTypeName()
                            }
                            "isNullable" -> isNullable = arg.value as Boolean
                        }
                    }

                    val finalType = type.copy(nullable = isNullable)
                    ctor.addParameter(name, finalType)
                    addProperty(
                        PropertySpec.builder(name, finalType)
                            .initializer(name)
                            .mutable(true)
                            .build()
                    )
                }

                primaryConstructor(ctor.build())

                addProperty(
                    PropertySpec.builder("intent", ClassName("android.content", "Intent"))
                        .addModifiers(KModifier.OVERRIDE)
                        .getter(
                            FunSpec.getterBuilder()
                                .addCode(buildIntentBlock(targetName, paramAnnotations))
                                .build()
                        )
                        .build()
                )

            }.build())
        }.build()

        codeGen.createNewFile(Dependencies(false), pkg, intentClassName).also {
            OutputStreamWriter(it, Charsets.UTF_8).use { writer ->
                fileSpec.writeTo(writer)
                writer.flush()
            }
        }
    }

    private fun buildIntentBlock(targetName: String, params: List<KSAnnotation>) = CodeBlock.builder().apply {
        add("return Intent(activity.get(), %L::class.java).apply {\n", targetName)

        for (annotation in params) {
            val name = annotation.arguments.first { it.name?.asString() == "name" }.value as String
            val type = annotation.arguments.first { it.name?.asString() == "type" }.value as KSType
            val isNullable = annotation.arguments.firstOrNull { it.name?.asString() == "isNullable" }?.value as? Boolean == true

            val kotlinType = type.toTypeName().copy(nullable = isNullable)
            val putExtraMethod = when (kotlinType) {
                STRING -> "putStringExtra"
                BOOLEAN -> "putBooleanExtra"
                INT -> "putIntExtra"
                LONG -> "putLongExtra"
                FLOAT -> "putFloatExtra"
                DOUBLE -> "putDoubleExtra"
                BYTE -> "putExtra" // byte is usually single, putByteExtra not in Intent
                CHAR -> "putExtra"
                SHORT -> "putExtra"
                else -> "putExtra"
            }

            if (isNullable) {
                add("this.%L?.let { %L(%S, it) }\n", name, putExtraMethod, name)
            } else {
                add("%L(%S, %L)\n", putExtraMethod, name, name)
            }
        }

        add("}\n")
    }.build()
}
