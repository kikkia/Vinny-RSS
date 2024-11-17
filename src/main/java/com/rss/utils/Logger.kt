package com.rss.utils

import com.kikkia.dislog.api.DislogClient
import com.kikkia.dislog.models.Log
import com.kikkia.dislog.models.LogLevel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.lang.Exception

class Logger(name: Class<*>) {

    private var logger: Logger = LoggerFactory.getLogger(name)

    @Autowired
    private var client: DislogClient? = null

    fun info(message:String) {
        sendDisLog(LogLevel.INFO, message)
        logger.info(message)
    }

    fun info(format: String, vararg args: Any) {
        info(format.format(args))
    }

    fun debug(message: String) {
        sendDisLog(LogLevel.DEBUG, message)
        logger.debug(message)
    }

    fun debug(format: String, vararg args: Any) {
        debug(format.format(args))
    }

    fun trace(message: String) {
        sendDisLog(LogLevel.TRACE, message)
        logger.trace(message)
    }

    fun trace(format: String, arg: Any) {
        sendDisLog(LogLevel.TRACE, format.format(arg))
    }

    fun warn(message: String) {
        warn(message, null)
    }

    fun warn(message: String, throwable: Throwable?) {
        sendDisLog(LogLevel.WARN, message, throwable)
        logger.warn(message)
    }

    fun warn(format: String, vararg args: Any) {
        warn(format.format(args))
    }

    fun error(message: String) {
        error(message, null)
    }

    fun error(message: String, throwable: Throwable?) {
        sendDisLog(LogLevel.ERROR, message, throwable)
        logger.error(message, throwable)
    }

    private fun sendDisLog(level: LogLevel, message: String, throwable: Throwable?) {
        if (client == null)
            client = SpringContext.getBean(DislogClient::class.java)

        val exception = if (throwable == null) null else Exception(throwable)

        client!!.queueLog(Log(message, level, exception))
    }

    private fun sendDisLog(level: LogLevel, message: String) {
        sendDisLog(level, message, null)
    }
}