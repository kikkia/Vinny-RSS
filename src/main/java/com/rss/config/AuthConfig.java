package com.rss.config;

import com.rss.config.properties.AuthProperties;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
public class AuthConfig implements HandlerInterceptor, WebMvcConfigurer {

    @Autowired
    private AuthProperties authProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getServletPath().equals("/error")) return true;

        String token = request.getHeader("Authorization");
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }

        if (authProperties.getClientIps() != null && authProperties.getClientIps().size() > 0) {
            if (!authProperties.getClientIps().contains(ipAddress)) {
                // IP not in whitelist
                response.setStatus(HttpStatus.SC_UNAUTHORIZED);
                System.out.println("Ip not in whitelist");
                return false;
            }
        }

        if (token == null) {
            response.setStatus(HttpStatus.SC_UNAUTHORIZED);
            System.out.println("Token is null");
            return false;
        }

        if (!token.equals(authProperties.getClientToken())) {
            response.setStatus(HttpStatus.SC_FORBIDDEN);
            System.out.println("Invalid Token");
            return false;
        }
        return true;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this);
    }
}
