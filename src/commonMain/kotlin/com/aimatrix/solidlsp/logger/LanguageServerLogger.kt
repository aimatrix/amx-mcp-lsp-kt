package com.aimatrix.solidlsp.logger

// Simplified logger stub
object LanguageServerLogger {
    fun info(message: String) {
        println("[INFO] $message")
    }
    
    fun error(message: String) {
        println("[ERROR] $message")
    }
    
    fun debug(message: String) {
        println("[DEBUG] $message")
    }
}