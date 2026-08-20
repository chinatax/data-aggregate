package com.dataagg.start.config;

import com.dataagg.common.result.Result;
import com.dataagg.start.annotation.RequireAuth;
import com.dataagg.start.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequireAuth requireAuth = handlerMethod.getMethodAnnotation(RequireAuth.class);
        if (requireAuth == null) {
            requireAuth = handlerMethod.getBeanType().getAnnotation(RequireAuth.class);
        }

        if (requireAuth == null) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeError(response, 401, "请先登录");
            return false;
        }

        String token = header.substring(7);
        try {
            Claims claims = jwtUtil.parseToken(token);
            String userIdStr = claims.getSubject();
            Long userId = userIdStr != null ? Long.valueOf(userIdStr) : null;
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);

            String[] requiredRoles = requireAuth.value();
            if (requiredRoles.length > 0) {
                List<String> roleList = Arrays.asList(requiredRoles);
                if (!roleList.contains(role)) {
                    writeError(response, 403, "权限不足");
                    return false;
                }
            }

            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            request.setAttribute("role", role);
            return true;

        } catch (ExpiredJwtException e) {
            writeError(response, 401, "登录已过期，请重新登录");
            return false;
        } catch (JwtException e) {
            writeError(response, 401, "无效的登录凭证");
            return false;
        }
    }

    private void writeError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        Result<Void> result = Result.error(message);
        result.setCode(status);
        objectMapper.writeValue(response.getWriter(), result);
    }
}
