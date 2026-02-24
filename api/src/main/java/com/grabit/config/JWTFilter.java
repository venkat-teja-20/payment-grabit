package com.grabit.config;

import com.grabit.Utilities.JWTUtil;
import com.grabit.Utilities.ModelMapperUtility;
import com.grabit.Utilities.Utility;
import com.grabit.bean.RoleDTO;
import com.grabit.enums.CommonErrors;
import com.grabit.exception.APIError;
import com.grabit.exception.CustomException;
import com.grabit.exception.JwtAuthenticationException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

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
            String role=null;
            List<Integer> permissions=null;
            if(authorizationHeader!=null && authorizationHeader.startsWith("JWT ")){
                token=authorizationHeader.substring(4);
                Claims claims= JWTUtil.getClaims(token);
                permissions=JWTUtil.getPermissions(claims);
                if (permissions == null || permissions.isEmpty()) {
                    throw new CustomException(Utility.buildErrorObject("PERMISSIONS_MISSING", "User do not have any valid permissions", 500, "jwtFilter"));
                }
                email=JWTUtil.getEmail(claims);
                role=JWTUtil.extractRole(claims);
            }
            if(email!=null && role!=null && SecurityContextHolder.getContext().getAuthentication()==null){
                List<SimpleGrantedAuthority> authorities=new ArrayList<>();
                try{
                    LinkedHashMap<String,Object> rolesMap= (LinkedHashMap<String, Object>) redisTemplate.opsForValue().get("role_"+role);
                    RoleDTO roles= ModelMapperUtility.map(rolesMap,RoleDTO.class);
                    if(Utility.isNullOrEmpty(roles) || Utility.isNullOrEmpty(roles.getRole()) || Utility.isNullOrEmpty(roles.getPermissions()))
                        throw new CustomException(Utility.buildErrorObject("INVALID_RESPONSE", "response received from auth service while fetching role details is null or not valid", 500, "JWTFilter"));
                    authorities.add(new SimpleGrantedAuthority("ROLE_"+roles.getRole().toValue()));
                    roles.getPermissions().forEach(permissionDTO -> {
                        authorities.add(new SimpleGrantedAuthority(permissionDTO.getPermission().toValue()));
                    });
                } catch (CustomException e) {
                    throw e;
                } catch (Exception e) {
                    log.error(e);
                    throw new CustomException(Utility.buildErrorObject("ROLE_FETCH_ERROR","Error while handling role details from redis",500,"JWTFilter"));
                }
                UsernamePasswordAuthenticationToken authenticationToken=new UsernamePasswordAuthenticationToken(email,null,authorities);
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
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
        } catch (Exception e) {
            log.error(e);
            response.setContentType("application/json");
            request.setAttribute("responseWriterFlag",true);
            if(e instanceof CustomException customException){
                response.setStatus(customException.getErrorObject().getHttpCode());
                response.getWriter().write(Utility.toJson(customException.getErrorObject().getErrorMsg()));
            }
            else {
                response.setStatus(500);
                response.getWriter().write(Utility.toJson(new APIError("SOMETHING_WENT_WRONG", e.getMessage())));
            }
        }
    }
}
