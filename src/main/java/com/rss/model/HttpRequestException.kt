package com.rss.model

import java.lang.Exception

class HttpRequestException(message: String?, val code: Int, cause: Throwable) : Exception(message, cause)