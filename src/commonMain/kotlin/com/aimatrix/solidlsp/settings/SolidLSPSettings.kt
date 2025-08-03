package com.aimatrix.solidlsp.settings

data class SolidLSPSettings(
    val timeout: Long = 30000L,
    val debug: Boolean = false
)