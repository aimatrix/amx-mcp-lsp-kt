package com.aimatrix.amxlsp

object Constants {
    const val DEFAULT_TOOL_TIMEOUT = 30000L
    const val DEFAULT_CONTEXT_SIZE = 1000
    const val CONFIG_FILE_NAME = "amxlsp.yaml"
    const val VERSION = "1.0.3"
    const val DEFAULT_ENCODING = "UTF-8"
    const val DEFAULT_CONTEXT = "default"
    const val DEFAULT_MODES = "default"
    const val AMXLSP_LOG_FORMAT = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    
    // Legacy alias for backward compatibility
    const val SERENA_LOG_FORMAT = AMXLSP_LOG_FORMAT
}