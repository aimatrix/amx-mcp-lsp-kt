package com.aimatrix.amxlsp.util

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import com.aimatrix.amxlsp.Constants
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * In-memory log handler that stores log messages
 */
class MemoryLogHandler : AppenderBase<ILoggingEvent>() {
    private val logs = ConcurrentLinkedQueue<LogEntry>()
    private val maxLogs = 10000
    
    override fun append(eventObject: ILoggingEvent) {
        val entry = LogEntry(
            timestamp = Instant.ofEpochMilli(eventObject.timeStamp),
            level = eventObject.level.toString(),
            loggerName = eventObject.loggerName,
            message = eventObject.formattedMessage,
            threadName = eventObject.threadName,
            throwable = eventObject.throwableProxy?.message
        )
        
        logs.offer(entry)
        
        // Remove old entries if we exceed the limit
        while (logs.size > maxLogs) {
            logs.poll()
        }
    }
    
    fun getLogs(): List<LogEntry> = logs.toList()
    
    fun clear() {
        logs.clear()
    }
    
    data class LogEntry(
        val timestamp: Instant,
        val level: String,
        val loggerName: String,
        val message: String,
        val threadName: String,
        val throwable: String? = null
    )
}

/**
 * Logging configuration utilities
 */
object LoggingConfig {
    fun configureLogging(logLevel: String = "INFO") {
        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        val rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME)
        
        // Set log level
        rootLogger.level = Level.toLevel(logLevel)
        
        // Configure pattern
        val encoder = PatternLayoutEncoder()
        encoder.context = context
        encoder.pattern = Constants.SERENA_LOG_FORMAT
        encoder.start()
        
        // Add memory handler
        val memoryHandler = MemoryLogHandler()
        memoryHandler.context = context
        memoryHandler.start()
        rootLogger.addAppender(memoryHandler)
    }
    
    fun setLogLevel(loggerName: String, level: String) {
        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        val logger = context.getLogger(loggerName)
        logger.level = Level.toLevel(level)
    }
    
    fun getMemoryHandler(): MemoryLogHandler? {
        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        val rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME)
        
        return rootLogger.iteratorForAppenders().asSequence()
            .filterIsInstance<MemoryLogHandler>()
            .firstOrNull()
    }
}