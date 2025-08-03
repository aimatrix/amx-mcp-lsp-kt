package com.aimatrix.amxlsp.util

import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Detect if we're running in a headless environment where GUI operations would fail.
 *
 * Returns true if:
 * - No DISPLAY variable on Linux/Unix
 * - Running in SSH session
 * - Running in WSL without X server
 * - Running in Docker container
 */
fun isHeadlessEnvironment(): Boolean {
    // Check if we're on Windows - GUI usually works there
    if (System.getProperty("os.name").lowercase().contains("windows")) {
        return false
    }
    
    // Check for DISPLAY variable (required for X11)
    if (System.getenv("DISPLAY") == null) {
        return true
    }
    
    // Check for SSH session
    if (System.getenv("SSH_CONNECTION") != null || System.getenv("SSH_CLIENT") != null) {
        return true
    }
    
    // Check for common CI/container environments
    if (System.getenv("CI") != null || System.getenv("CONTAINER") != null || File("/.dockerenv").exists()) {
        return true
    }
    
    // Check for WSL
    val osRelease = File("/proc/version")
    if (osRelease.exists()) {
        try {
            val content = osRelease.readText().lowercase()
            if ("microsoft" in content) {
                // In WSL, even with DISPLAY set, X server might not be running
                return true
            }
        } catch (e: Exception) {
            // Ignore read errors
        }
    }
    
    return false
}

/**
 * Shows the given exception in the GUI log viewer on the main thread and ensures that the exception is logged or at
 * least printed to stderr.
 */
fun showFatalExceptionSafe(e: Exception) {
    // Log the error and print it to stderr
    logger.error(e) { "Fatal exception: $e" }
    System.err.println("Fatal exception: $e")
    
    // Don't attempt GUI in headless environments
    if (isHeadlessEnvironment()) {
        logger.debug { "Skipping GUI error display in headless environment" }
        return
    }
    
    // attempt to show the error in the GUI
    try {
        // NOTE: The import can fail on macOS if Tk is not available (depends on Python interpreter installation, which uv
        //   used as a base); while tkinter as such is always available, its dependencies can be unavailable on macOS.
        // In Kotlin, we would use a different GUI framework like JavaFX or Swing
        showFatalException(e)
    } catch (guiError: Exception) {
        logger.debug { "Failed to show GUI error dialog: $guiError" }
    }
}

// This would be implemented in a separate GUI module
private fun showFatalException(e: Exception) {
    // TODO: Implement using JavaFX or Swing
    throw UnsupportedOperationException("GUI error display not yet implemented")
}