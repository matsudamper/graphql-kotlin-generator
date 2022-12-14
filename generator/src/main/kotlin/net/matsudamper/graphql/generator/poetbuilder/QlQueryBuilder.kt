package net.matsudamper.graphql.generator.poetbuilder

import com.squareup.kotlinpoet.*
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLObjectType
import net.matsudamper.graphql.generator.util.*

internal class QlQueryBuilder(
    private val type: GraphQLObjectType,
    private val typeDefine: TypeDefinition,
) {
    fun build(): TypeSpec {
        val queryClassName = ClassNames.QlQuery.getClassName(type.name)

        println("============${type.name}=======")

        val typeSpec = TypeSpec.classBuilder(queryClassName)
            .addKdoc("Generated by ${QlQueryBuilder::class.simpleName}")
            .addModifiers(KModifier.ABSTRACT)
            .createConstructor()
            .createBody()
            .build()

        return typeSpec
    }

    /***
     *
     * abstract fun ResponseScope<QlBaseReportInterface>.getUser(argument: Boolean, inputType: InputType): CompletableFuture<QlBaseUserInterface>
     *
     * fun getUser(): CompletableFuture<QlBaseUserInterface> {
     *     val argument = environment.arguments["argument"] as Boolean
     *     val inputType = InputType.fromInput(environment.arguments["input_type"]) as InputType
     *     return with(ResponseScope(environment.getSource<QlBaseReportInterface>())) {
     *         getUser(
     *             argument = argument,
     *             inputType = inputType,
     *         )
     *     }
     * }
     */
    private fun TypeSpec.Builder.createBody(): TypeSpec.Builder {
        val fieldObject = GraphQlUtil.getFieldQueryTypes(type)

        fieldObject.map { field ->
            val functionName = buildString {
                append("get")
                append(field.name.take(1).toUpperCase())
                append(field.name.drop(1))
            }

            val returns = ClassNames.CompletableStage(
                run {
                    val element = KotlinTypeStruct.fromGraphQlType(field.type)
                    element.getTypeName {
                        typeDefine.getQueryReturnType(it.namedType)
                    }
                }
            )

            val baseType = typeDefine.getClassName(KotlinTypeStruct.fromGraphQlType(type).getCoreNamedObject().name)

            run {
                val abstract = FunSpec.builder(functionName)
                    .addModifiers(KModifier.ABSTRACT)
                    .returns(returns)
                    .receiver(ClassNames.ResponseScope.getClassName(baseType))
                    .addParameters(
                        field.arguments.map {
                            val argumentType = KotlinTypeStruct.fromGraphQlType(it.type).getTypeName(typeDefine)

                            ParameterSpec(it.name, argumentType)
                        }
                    )
                    .build()

                addFunction(abstract)
            }

            run {
                val funSpec = FunSpec.builder(functionName)
                    .addCode(createArgument(field.arguments))
                    .addCode(createReturnBlock(field, functionName, baseType))
                    .returns(returns)
                    .build()

                addFunction(funSpec)
            }
        }

        return this
    }

    /**
     * return with(ResponseScope(environment.getSource<Report>())) {
     *     getUser(argument = argument)
     * }
     */
    private fun createReturnBlock(field: GraphQLFieldDefinition, functionName: String, baseType: TypeName): CodeBlock {
        return CodeBlock.Builder().apply {
            beginControlFlow("return with(ResponseScope(environment.getSource<%T>()))", baseType)
            addStatement("${functionName}(")
            withIndent {
                field.arguments.forEach {
                    addStatement("${it.name} = ${it.name},")
                }
            }
            addStatement(")")
            endControlFlow()
        }.build()
    }

    /**
     * val argument = environment.arguments["argument"] as Boolean
     * val inputType = InputType.fromInput(environment.arguments["input_type"]) as InputType
     */
    private fun createArgument(arguments: List<GraphQLArgument>): CodeBlock {
        return CodeBlock.Builder().apply {
            arguments.forEach { argument ->
                val name = argument.name
                when (argument.type) {
                    is GraphQLEnumType,
                    is GraphQLInputType -> {
                        add("val $name = ")
                        add(
                            GraphQlToPoetUtil.parseList(
                                type = argument.type,
                                option = GraphQlToPoetUtil.ParseListOption(
                                    singleObject = GraphQlToPoetUtil.ParseListOption.SingleObject(
                                        coreBlock = { coreType ->
                                            when (coreType.type) {
                                                is GraphQLEnumType,
                                                is GraphQLInputObjectType -> {
                                                    add(
                                                        if (coreType.type is GraphQLEnumType) {
                                                            """%T.fromInput(environment.arguments["$name"] as String)"""
                                                        }else{
                                                            """%T.fromInput(environment.arguments["$name"])"""
                                                        },
                                                        typeDefine.getClassName(coreType.namedType.name)
                                                    )
                                                    if (coreType.isNull) {
                                                        add(" as? ")
                                                    } else {
                                                        add(" as ")
                                                    }
                                                    add("%T", typeDefine.getClassName(coreType.namedType.name))
                                                }
                                                else -> {
                                                    add("""environment.arguments["$name"] as""")
                                                    if (coreType.isNull) {
                                                        add("?")
                                                    }
                                                    add(" %T", typeDefine.getClassName(coreType.namedType.name))
                                                }
                                            }
                                        }
                                    ),
                                    listObject = GraphQlToPoetUtil.ParseListOption.ListObject(
                                        receiver = {
                                            """environment.arguments["$name"]"""
                                        },
                                        coreBlock = { coreType ->
                                            val kotlinFieldElement =
                                                KotlinTypeStruct.fromGraphQlType(coreType.type)
                                            val type = kotlinFieldElement.getTypeName(typeDefine)

                                            when (val inputType = coreType.type) {
                                                is GraphQLInputObjectType -> {
                                                    addStatement(
                                                        """${type}.fromInput(it as Any?) as %T""",
                                                        type.copy(nullable = coreType.isNull)
                                                    )
                                                }
                                                is GraphQLEnumType -> {
                                                    addStatement(
                                                        """${type}.fromInput(it as String?) as %T""",
                                                        type.copy(nullable = coreType.isNull)
                                                    )
                                                }
                                                else -> {
                                                    addStatement(
                                                        """it as %T""",
                                                        type.copy(nullable = coreType.isNull),
                                                    )
                                                }
                                            }
                                        },
                                    )
                                ),
                            )
                        )
                        add("\n")
                    }
                    else -> {
                        addStatement(
                            """val $name = environment.arguments["$name"] as ${
                                KotlinTypeStruct.fromGraphQlType(
                                    argument.type
                                ).getTypeName(typeDefine)
                            }"""
                        )
                    }
                }
            }
        }.build()
    }

    /***
     * abstract class QlDynamicUser @Suppress("UNUSED_PARAMETER") constructor(
     *     private val environment: DataFetchingEnvironment,
     * ) {
     */
    private fun TypeSpec.Builder.createConstructor(): TypeSpec.Builder {
        return addPrimaryConstructor(
            listOf(
                PoetUtil.PrimaryConstructorElement(
                    name = "environment",
                    typeName = ClassNames.DataFetchingEnvironment,
                    override = false,
                    isProperty = true,
                    isPrivate = true,
                )
            )
        )
    }

}