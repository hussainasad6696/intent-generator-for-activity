package com.example.intent_generator

import com.example.intent_generator.annotation_function_builder.AnnotationFunctionBuilder
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
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
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
                val annotationFunctionBuilder =
                    AnnotationFunctionBuilder(
                        logger,
                        classDecl.packageName.asString(),
                        "${classDecl.simpleName.asString()}Intent"
                    )
                logger.info("Present in intent processor forEach $classDecl")
                generateClass(classDecl, annotationFunctionBuilder)
            }
        logger.warn("🛠️ IntentProcessor is completed...")
        return emptyList()
    }

    private fun generateClass(
        classDecl: KSClassDeclaration,
        annotationFunctionBuilder: AnnotationFunctionBuilder
    ) {
        val annotation = classDecl.annotations.first { it.shortName.asString() == "GenerateIntent" }

        val paramsValue =
            annotation.arguments.firstOrNull { it.name?.asString() == "params" }?.value
        val resultCodeValue =
            annotation.arguments.firstOrNull { it.name?.asString() == "resultCode" }?.value as? Int
                ?: 0
        val paramAnnotations = paramsValue as? List<KSAnnotation> ?: emptyList()

        val targetName = classDecl.simpleName.asString()

        val activityType = ClassName("java.lang.ref", "WeakReference")
            .parameterizedBy(ClassName("android.app", "Activity"))

        val standardProps = listOf(
            "activity" to activityType,
            "hasResultCode" to BOOLEAN,
            "resultCode" to INT,
            "animate" to BOOLEAN,
            "finish" to BOOLEAN,
            "clearTop" to BOOLEAN,
            "newTask" to BOOLEAN
        )

        // Separate nullable and non-nullable intent params
        val intentParam = paramAnnotations.map { annotation ->
            var name = ""
            var type: TypeName = UNIT
            var isNullable = true
            var typeArg: TypeName = UNIT
            var defaultValue = ""

            for (arg in annotation.arguments) {
                when (arg.name?.asString()) {
                    "name" -> name = arg.value as String
                    "type" -> type = (arg.value as KSType).toTypeName()
                    "typeArg" -> typeArg = (arg.value as KSType).toTypeName()
                    "isNullable" -> isNullable = arg.value as Boolean
                    "defaultValue" -> defaultValue = arg.value as String
                }
            }

            // Build type like ArrayList<Uri> instead of ArrayList<*>
            val finalType = if (type is ParameterizedTypeName) {
                type.rawType.parameterizedBy(typeArg)
            } else type

//            name to finalType.copy(nullable = isNullable)
            IntentParam(name, finalType.copy(nullable = isNullable), isNullable, defaultValue)
        }

        val (nonNullableParams, nullableParams) = intentParam.partition { !it.isNullable }

        val fileSpec =
            FileSpec.builder(annotationFunctionBuilder.pkg, annotationFunctionBuilder.className)
                .apply {
                    addType(
                        TypeSpec.classBuilder(annotationFunctionBuilder.className).apply typeSpec@{
                            addKdoc(
                                "${annotationFunctionBuilder.className} deals with intent handling so that the developer " +
                                        "can know which values are must for the activity" +
                                        " to function and which are optional. " +
                                        "Where parameters like \n@param activity: Required weak reference to the activity," +
                                        "\n@param hasResultCode: if the activity has a result code or not so deal with starting activity as per the result code," +
                                        "\n@param resultCode: the actual result code," +
                                        "\n@param animate: if the activity has to be animated," +
                                        "\n@param finish: if the current activity has to be finished or not," +
                                        "\n@param clearTop: clear to the top of next activity," +
                                        "\n@param newTask: start a new task"
                            )

                            superclass(
                                ClassName(
                                    "com.example.intentgenerationsample",
                                    "IntentHandler"
                                )
                            )

                            val modifiedStandardProps = standardProps.map { (name, typeName) ->
                                val default = if (
                                    name == "hasResultCode" || name == "animate" ||
                                    name == "finish" || name == "clearTop" || name == "newTask"
                                ) "false" else ""

                                IntentParam(
                                    name = name,
                                    type = typeName,
                                    isNullable = false,
                                    defaultValue = default
                                )
                            }

                            // Create primary constructor
                            this@typeSpec.primaryConstructor(
                                annotationFunctionBuilder.primaryConstructor(
                                    modifiedStandardProps = modifiedStandardProps,
                                    nonNullableParams = nonNullableParams,
                                    nullableParams = nullableParams,
                                    resultCodeValue = resultCodeValue,
                                    typeSpecs = this@typeSpec
                                )
                            )

                            addType(
                                annotationFunctionBuilder.companionBuilder(
                                    nonNullableParams = nonNullableParams,
                                    resultCodeValue = resultCodeValue
                                )
                            )

                            // Create secondary constructor
                            // Only create secondary constructor if there are nullable params
                            if (nullableParams.isNotEmpty()) {
                                addFunction(
                                    annotationFunctionBuilder.secondaryConstructorBuilder(
                                        modifiedStandardProps = modifiedStandardProps,
                                        nonNullableParams = nonNullableParams,
                                        nullableParams = nullableParams
                                    )
                                )
                            }

                            // Add intent property with override
                            addProperty(
                                PropertySpec.builder(
                                    "intent",
                                    ClassName("android.content", "Intent")
                                )
                                    .addModifiers(KModifier.OVERRIDE)
                                    .getter(
                                        annotationFunctionBuilder.intentBlockBuilder(
                                            targetName,
                                            paramAnnotations
                                        )
                                    )
                                    .build()
                            )

                            // === Add getDataHandler function ===
                            addFunction(
                                annotationFunctionBuilder.getDataHandlerBuilder(
                                    resultCodeValue = resultCodeValue,
                                    standardProps = modifiedStandardProps,
                                    nonNullableParams = nonNullableParams,
                                    nullableParams = nullableParams,
                                    paramAnnotations = paramAnnotations
                                )
                            )
                        }.build()
                    )
                }.build()

        // Write to file
        codeGen.createNewFile(
            Dependencies.ALL_FILES,
            annotationFunctionBuilder.pkg,
            annotationFunctionBuilder.className
        ).also {
            OutputStreamWriter(it, Charsets.UTF_8).use { writer ->
                fileSpec.writeTo(writer)
                writer.flush()
            }
        }
    }
}