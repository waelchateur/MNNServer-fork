package io.kindbrave.mnnserver.webserver.utils

import com.elvishew.xlog.XLog
import io.kindbrave.mnnserver.webserver.request.FunctionTool
import io.kindbrave.mnnserver.webserver.response.FunctionCall
import io.kindbrave.mnnserver.webserver.response.ToolCall
import kotlinx.serialization.json.Json
import kotlin.collections.forEach

object FunctionCallUtils {
    private val tag = FunctionCallUtils::class.java.simpleName

//    fun buildFunctionCallPrompt(tools: List<FunctionTool>): String {
//        val builder = StringBuilder()
//        builder.appendLine("You are a helpful assistant capable of calling functions to solve tasks.")
//        builder.appendLine("You have access to the following functions:")
//
//        tools.forEach { tool ->
//            builder.appendLine("- Name: ${tool.function.name}")
//            builder.appendLine("  Description: ${tool.function.description}")
//            builder.appendLine("  Parameters (JSON Schema): ${tool.function.parameters}")
//            builder.appendLine()
//        }
//
//        builder.appendLine("When appropriate, you must respond **only** with a JSON array matching this structure:")
//        builder.appendLine()
//        builder.appendLine("[")
//        builder.appendLine("  {")
//        builder.appendLine("    \"name\": \"<name of the selected tool>\",")
//        builder.appendLine("    \"arguments\": <parameters matching the tool's JSON schema>")
//        builder.appendLine("  },")
//        builder.appendLine("  ... // You may return multiple function calls in this list if needed")
//        builder.appendLine("]")
//        builder.appendLine()
//        builder.appendLine("Do not include any text outside of the JSON array.")
//
//        return builder.toString()
//    }

    fun buildFunctionCallPrompt(tools: List<FunctionTool>): String {
        val builder = StringBuilder()
        builder.appendLine("You are a helpful assistant capable of calling functions to solve tasks and having normal conversations.")
        builder.appendLine("You have access to the following functions:")

        tools.forEach { tool ->
            builder.appendLine("- Name: ${tool.function.name}")
            builder.appendLine("  Description: ${tool.function.description}")
            builder.appendLine("  Parameters (JSON Schema): ${tool.function.parameters}")
            builder.appendLine()
        }

        builder.appendLine("When you need to call a function, respond with a JSON array matching this structure:")
        builder.appendLine()
        builder.appendLine("[")
        builder.appendLine("  {")
        builder.appendLine("    \"name\": \"<name of the selected tool>\",")
        builder.appendLine("    \"arguments\": <parameters matching the tool's JSON schema>")
        builder.appendLine("  },")
        builder.appendLine("  ... // You may return multiple function calls in this list if needed")
        builder.appendLine("]")
        builder.appendLine()
        builder.appendLine("If a function call is not necessary, respond with normal text to answer the user's question or engage in conversation.")

        return builder.toString()
    }

    fun tryToParseToolCall(response: String): List<ToolCall>? {
        return try {
            val functionCalls = Json.decodeFromString<List<FunctionCall>>(response)
            functionCalls.mapIndexed { index, call ->
                ToolCall(
                    id = "${call.name}_$index",
                    index = index,
                    toolType = "function",
                    functionCall = call
                )
            }
        } catch (e: Exception) {
            XLog.tag(tag).e("Failed to parse function call response: $response", e)
            null
        }
    }
}