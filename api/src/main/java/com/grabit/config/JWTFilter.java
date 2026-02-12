package com.grabit.config;

import com.grabit.Utilities.Utility;
import com.grabit.enums.CommonErrors;
import com.grabit.exception.APIError;
import com.grabit.exception.JwtAuthenticationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Log4j2
public class JWTFilter extends OncePerRequestFilter {
    private RedisTemplate<String,Object> redisTemplate;

    public JWTFilter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String authorizationHeader= Utility.isNullOrEmpty(request.getHeader("Authorization"))?request.getHeader("authorization"):request.getHeader("Authorization");
            String token=null;
            String email=null;
            if(authorizationHeader!=null && authorizationHeader.startsWith("JWT ")){
                token=authorizationHeader.substring(4);
            }
            filterChain.doFilter(request,response);
        } catch (JwtAuthenticationException e){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            APIError apiError=e.getAuthenticationError();
            if(Utility.isNullOrEmpty(apiError))
                apiError=new APIError(CommonErrors.AUTHENTICATION_ERROR.toString(),CommonErrors.AUTHENTICATION_ERROR.getMessage());
            log.error("Authentication Unsuccessful : "+Utility.toJson(apiError));
            request.setAttribute("responseWriterFlag",true);
            response.getWriter().write(Utility.toJson(apiError));
        }
    }
}
