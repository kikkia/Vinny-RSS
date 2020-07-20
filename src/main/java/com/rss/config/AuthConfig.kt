package com.rss.config

import com.rss.config.properties.AuthProperties
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Configuration
open class AuthConfig : HandlerInterceptor, WebMvcConfigurer {
    @Autowired
    private val authProperties: AuthProperties? = null

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (request.servletPath == "/error") return true
        val token = request.getHeader("Authorization")
        var ipAddress = request.getHeader("X-FORWARDED-FOR")
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.remoteAddr
        }
        if (authProperties!!.clientIps != null && authProperties.clientIps!!.size > 0) {
            if (!authProperties.clientIps!!.contains(ipAddress)) { // IP not in whitelist
                response.status = HttpStatus.SC_UNAUTHORIZED
                println("Ip not in whitelist")
                return false
            }
        }
        if (token == null) {
            response.status = HttpStatus.SC_UNAUTHORIZED
            println("Token is null")
            return false
        }
        if (token != authProperties.clientToken) {
            response.status = HttpStatus.SC_FORBIDDEN
            println("Invalid Token")
            return false
        }
        return true
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(this)
    }
}