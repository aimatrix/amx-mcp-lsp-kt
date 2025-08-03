package com.aimatrix.amxlsp.prompt

interface PromptFactory {
    fun createPrompt(template: String, variables: Map<String, Any>): String
}

class DefaultPromptFactory : PromptFactory {
    override fun createPrompt(template: String, variables: Map<String, Any>): String {
        var result = template
        variables.forEach { (key, value) ->
            result = result.replace("{$key}", value.toString())
        }
        return result
    }
}