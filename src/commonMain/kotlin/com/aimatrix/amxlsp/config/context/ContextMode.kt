package com.aimatrix.amxlsp.config.context

import com.aimatrix.amxlsp.tools.Tool
import kotlin.reflect.KClass

interface AmxLspAgentContext {
    val name: String
    fun getEnabledTools(): Set<KClass<out Tool>>
}

interface AmxLspAgentMode {
    val name: String
    fun getEnabledTools(): Set<KClass<out Tool>>
    fun getDisabledTools(): Set<KClass<out Tool>>
}

class DefaultContext : AmxLspAgentContext {
    override val name = "desktop-app"
    
    override fun getEnabledTools(): Set<KClass<out Tool>> {
        // TODO: Return actual tool classes based on context configuration
        return emptySet()
    }
}

class InteractiveMode : AmxLspAgentMode {
    override val name = "interactive"
    
    override fun getEnabledTools(): Set<KClass<out Tool>> {
        // TODO: Return tools enabled in interactive mode
        return emptySet()
    }
    
    override fun getDisabledTools(): Set<KClass<out Tool>> {
        // TODO: Return tools disabled in interactive mode
        return emptySet()
    }
}

class EditingMode : AmxLspAgentMode {
    override val name = "editing"
    
    override fun getEnabledTools(): Set<KClass<out Tool>> {
        // TODO: Return tools enabled in editing mode
        return emptySet()
    }
    
    override fun getDisabledTools(): Set<KClass<out Tool>> {
        // TODO: Return tools disabled in editing mode
        return emptySet()
    }
}