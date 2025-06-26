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
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.CHAR_SEQUENCE
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.toTypeName
import java.io.File
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

        val targetType =
            annotation.arguments.first { it.name?.asString() == "target" }.value as KSType
        val paramsValue =
            annotation.arguments.firstOrNull { it.name?.asString() == "params" }?.value
        val resultCodeValue = annotation.arguments.firstOrNull { it.name?.asString() == "resultCode" }?.value as? Int ?: 0
        val paramAnnotations = paramsValue as? List<KSAnnotation> ?: emptyList()

        val targetName = targetType.declaration.simpleName.asString()
        val pkg = classDecl.packageName.asString()
        val intentClassName = "${targetName}Intent"

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

        // Separate nullable and non-nullable intent params
        val (nonNullableParams, nullableParams) = paramAnnotations.map { annotation ->
            var name = ""
            var type: TypeName = UNIT
            var isNullable = true
            var typeArg: TypeName = UNIT

            for (arg in annotation.arguments) {
                when (arg.name?.asString()) {
                    "name" -> name = arg.value as String
                    "type" -> type = (arg.value as KSType).toTypeName()
                    "typeArg" -> typeArg = (arg.value as KSType).toTypeName()
                    "isNullable" -> isNullable = arg.value as Boolean
                }
            }

            // Build type like ArrayList<Uri> instead of ArrayList<*>
            val finalType = if (type is ParameterizedTypeName) {
                type.rawType.parameterizedBy(typeArg)
            } else type

            name to finalType.copy(nullable = isNullable)
        }.partition { !it.second.isNullable }

        val fileSpec = FileSpec.builder(pkg, intentClassName).apply {
            addType(TypeSpec.classBuilder(intentClassName).apply {
                superclass(ClassName("com.example.intentgenerationsample", "IntentHandler"))

                // Create primary constructor
                val primaryCtor = FunSpec.constructorBuilder()
                (standardProps + nonNullableParams).forEach { (name, type) ->
                    val paramBuilder = ParameterSpec.builder(name, type)
                    if (name == "resultCode") {
                        paramBuilder.defaultValue("%L", resultCodeValue)
                    }
                    primaryCtor.addParameter(paramBuilder.build())
                    addProperty(
                        PropertySpec.builder(name, type)
                            .initializer(name)
                            .apply {
                                if (standardProps.any { it.first == name }) addModifiers(KModifier.OVERRIDE)
                                else mutable(true)
                            }
                            .build()
                    )
                }

                addType(buildCompanion(
                    pkg = pkg,
                    intentClassName = intentClassName,
                    nonNullableParams = nonNullableParams,
                    resultCodeValue = resultCodeValue // add this param
                ))

                // Add nullable properties (default null)
                nullableParams.forEach { (name, type) ->
                    addProperty(
                        PropertySpec.builder(name, type)
                            .initializer("null")
                            .mutable(true)
                            .build()
                    )
                }

                primaryConstructor(primaryCtor.build())

                // Create secondary constructor
                if (nullableParams.isNotEmpty()) {
                    val codeBlock = CodeBlock.builder()
                    nullableParams.forEach { (name, _) ->
                        codeBlock.addStatement("this.%L = %L", name, name)
                    }
                    addFunction(
                        FunSpec.constructorBuilder()
                            .addParameters((standardProps + nonNullableParams + nullableParams).map { (name, type) ->
                                ParameterSpec.builder(name, type).build()
                            })
                            .callThisConstructor(*(standardProps + nonNullableParams).map { it.first }
                                .toTypedArray())
                            .addCode(codeBlock.build())
                            .build()
                    )
                }

                // Add intent property with override
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

                // === Add getDataHandler function ===
                addFunction(
                    FunSpec.builder("getDataHandler")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter(
                            "intent",
                            ClassName("android.content", "Intent").copy(nullable = true)
                        )
                        .returns(ClassName(pkg, intentClassName))
                        .addCode(
                            buildGetDataHandlerCodeBlock(
                                pkg = pkg,
                                intentClassName = intentClassName,
                                resultCodeValue = resultCodeValue,
                                standardProps = standardProps,
                                nonNullableParams = nonNullableParams,
                                nullableParams = nullableParams,
                                paramAnnotations = paramAnnotations
                            )
                        )
                        .build()
                )
            }.build())
        }.build()

        // Write to file
        codeGen.createNewFile(Dependencies(false), pkg, intentClassName).also {
            OutputStreamWriter(it, Charsets.UTF_8).use { writer ->
                fileSpec.writeTo(writer)
                writer.flush()
            }
        }
    }

    private fun buildCompanion(
        pkg: String,
        intentClassName: String,
        nonNullableParams: List<Pair<String, TypeName>>,
        resultCodeValue: Int
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
                            // Add default values for non-nullable params
                            nonNullableParams.forEach { (name, type) ->
                                val defaultValue = when {
                                    type.toString().startsWith("kotlin.String") -> "\"\""
                                    type.toString().startsWith("kotlin.Boolean") -> "false"
                                    type.toString().startsWith("kotlin.Int") -> "0"
                                    type.toString().startsWith("kotlin.Long") -> "0L"
                                    type.toString().startsWith("kotlin.Float") -> "0f"
                                    type.toString().startsWith("kotlin.Double") -> "0.0"
                                    type.toString().startsWith("kotlin.Short") -> "0"
                                    type.toString().startsWith("kotlin.Byte") -> "0"
                                    type.toString().startsWith("kotlin.Char") -> "'\\u0000'"
                                    type is ParameterizedTypeName && type.rawType.simpleName == "ArrayList" -> "arrayListOf()"
                                    else -> "null"
                                }
                                add("    %L = %L,\n", name, defaultValue)
                            }
                            add(")\n")
                        }.build()
                    )
                    .build()
            )
            .build()
    }

    // Helper function to build the getDataHandler function body
    private fun buildGetDataHandlerCodeBlock(
        pkg: String,
        intentClassName: String,
        resultCodeValue: Int,
        standardProps: List<Pair<String, TypeName>>,
        nonNullableParams: List<Pair<String, TypeName>>,
        nullableParams: List<Pair<String, TypeName>>,
        paramAnnotations: List<KSAnnotation>
    ): CodeBlock {
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
                    when {
                        qualifiedNameForTypeArg == "kotlin.Int" || qualifiedNameForTypeArg == "kotlin.Int?" || kotlinType == INT || kotlinType == INT.copy(nullable = true) -> "getIntegerArrayListExtra"
                        qualifiedNameForTypeArg == "kotlin.String" || qualifiedNameForTypeArg == "kotlin.String?" || kotlinType == STRING || kotlinType == STRING.copy(nullable = true) -> "getStringArrayListExtra"
                        qualifiedNameForTypeArg == "kotlin.CharSequence" || qualifiedNameForTypeArg == "kotlin.CharSequence?" || kotlinType == CHAR_SEQUENCE || kotlinType == CHAR_SEQUENCE.copy(nullable = true) -> "getCharSequenceArrayListExtra"
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

        return CodeBlock.builder().apply {
            addStatement("val intent = intent ?: activity.get()?.intent")
            add("return %L(\n", intentClassName)

            val primaryParams = standardProps + nonNullableParams
            primaryParams.forEachIndexed { i, (name, type) ->
                val method = getReturnIntentType(name, type)

                val valueExpr = when {
                    name == "activity" -> "activity"
                    name == "resultCode" -> "$resultCodeValue"
                    name == "animate" -> "false"
                    name == "finish" -> "false"
                    name == "clearTop" -> "false"
                    method.startsWith("getParcelableArrayListExtra") -> {
                        val generic = method.substringAfter("<").substringBefore(">")
                        "intent?.getParcelableArrayListExtra<$generic>(\"$name\") ?: arrayListOf()"
                    }
                    method == "getBooleanExtra" -> "intent?.$method(\"$name\", false) ?: false"
                    method == "getIntExtra" -> "intent?.$method(\"$name\", 0) ?: 0"
                    method == "getLongExtra" -> "intent?.$method(\"$name\", 0L) ?: 0L"
                    method == "getFloatExtra" -> "intent?.$method(\"$name\", 0f) ?: 0f"
                    method == "getDoubleExtra" -> "intent?.$method(\"$name\", 0.0) ?: 0.0"
                    method == "getShortExtra" -> "intent?.$method(\"$name\", 0.toShort()) ?: 0.toShort()"
                    method == "getByteExtra" -> "intent?.$method(\"$name\", 0.toByte()) ?: 0.toByte()"
                    method == "getCharExtra" -> "intent?.$method(\"$name\", '\\u0000') ?: '\\u0000'"
                    else -> "intent?.$method(\"$name\") as? $type"
                }

                val comma = if (i < primaryParams.size - 1) "," else ""
                addStatement("    %L = %L$comma", name, valueExpr)
            }

            add(").apply {\n")

            nullableParams.forEach { (name, type) ->
                val method = getReturnIntentType(name, type)

                val statement = when {
                    method.startsWith("getParcelableArrayListExtra") -> {
                        val generic = method.substringAfter("<").substringBefore(">")
                        "intent?.getParcelableArrayListExtra<$generic>(\"$name\")"
                    }

                    method == "getBooleanExtra" -> "intent?.getBooleanExtra(\"$name\", false) ?: false"
                    method == "getIntExtra" -> "intent?.getIntExtra(\"$name\", 0) ?: 0"
                    method == "getLongExtra" -> "intent?.getLongExtra(\"$name\", 0L) ?: 0L"
                    method == "getFloatExtra" -> "intent?.getFloatExtra(\"$name\", 0f) ?: 0f"
                    method == "getDoubleExtra" -> "intent?.getDoubleExtra(\"$name\", 0.0) ?: 0.0"
                    method == "getShortExtra" -> "intent?.getShortExtra(\"$name\", 0) ?: 0"
                    method == "getByteExtra" -> "intent?.getByteExtra(\"$name\", 0) ?: 0"
                    method == "getCharExtra" -> "intent?.getCharExtra(\"$name\", '\\u0000') ?: '\\u0000'"

                    method == "getSerializableExtra" -> "intent?.$method(\"$name\") as? $type"
                    else -> "intent?.$method(\"$name\")"
                }

                addStatement("    this.%L = %L", name, statement)
            }

            add("}\n")
        }.build()
    }

    private fun KSAnnotation.getString(key: String): String? =
        arguments.firstOrNull { it.name?.asString() == key }?.value as? String

    private fun KSAnnotation.getBoolean(key: String): Boolean? =
        arguments.firstOrNull { it.name?.asString() == key }?.value as? Boolean

    private fun KSAnnotation.getKSTypeOrNull(key: String): KSType? =
        arguments.firstOrNull { it.name?.asString() == key }?.value as? KSType


    private fun buildIntentBlock(targetName: String, params: List<KSAnnotation>) =
        CodeBlock.builder().apply {
            add(
                "return activity.get()?.let { Intent(activity.get(), %L::class.java).apply {\n",
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

                    return when {
                        qualifiedName == "kotlin.Int" || qualifiedName == "kotlin.Int?" ||
                                kotlinType == INT || kotlinType == INT.copy(nullable = true) -> {
                            "putIntegerArrayListExtra"
                        }

                        qualifiedName == "kotlin.String" || qualifiedName == "kotlin.String?" ||
                                kotlinType == STRING || kotlinType == STRING.copy(nullable = true) -> {
                            "putStringArrayListExtra"
                        }

                        qualifiedName == "kotlin.CharSequence" || qualifiedName == "kotlin.CharSequence?" ||
                                kotlinType == CHAR_SEQUENCE || kotlinType == CHAR_SEQUENCE.copy(
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

            add("} \n } ?: throw ActivityReferenceEmptyException()\n")
        }.build()

}
