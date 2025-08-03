package com.aimatrix.solidlsp.exceptions

/**
 * Base exception for all SolidLSP errors
 */
open class SolidLSPException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Thrown when language server initialization fails
 */
class LanguageServerInitException(
    message: String,
    cause: Throwable? = null
) : SolidLSPException(message, cause)

/**
 * Thrown when a language server request times out
 */
class LanguageServerTimeoutException(
    message: String,
    cause: Throwable? = null
) : SolidLSPException(message, cause)

/**
 * Thrown when the language server is not ready
 */
class LanguageServerNotReadyException(
    message: String
) : SolidLSPException(message)

/**
 * Thrown when a language is not supported
 */
class UnsupportedLanguageException(
    language: String
) : SolidLSPException("Language not supported: $language")