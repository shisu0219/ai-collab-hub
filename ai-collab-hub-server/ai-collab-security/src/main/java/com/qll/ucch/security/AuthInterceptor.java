package com.qll.ucch.security;

import com.qll.ucch.constance.redis.RedisKeyConst;
import com.qll.ucch.exception.UnauthorizedException;
import com.qll.ucch.models.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 登录拦截器。
 * 检查请求头里的 token，验过了就把用户信息塞进 SecurityContextHolder，
 * 后面的 Controller / Service 可以直接用。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    private final StringRedisTemplate redisTemplate;

    /** 直接的 JSON 序列化工具，写 401 响应要用 */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 静态资源和跨域预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 方法或类上标了 @IgnoreAuth 的，不校验
        if (handlerMethod.hasMethodAnnotation(IgnoreAuth.class)) {
            return true;
        }
        if (handlerMethod.getBeanType().isAnnotationPresent(IgnoreAuth.class)) {
            return true;
        }

        String token = resolveToken(request);
        if (token == null || !jwtUtil.valid(token)) {
            writeUnauthorized(response, "登录已失效，请重新登录");
            return false;
        }

        // 检查 Redis 里的登录态还在不在（支持管理员踢人下线）
        Long userId = jwtUtil.getUserId(token);
        Boolean hasLogin = redisTemplate.hasKey(RedisKeyConst.LOGIN_USER_PREFIX + userId);
        if (Boolean.FALSE.equals(hasLogin)) {
            writeUnauthorized(response, "登录已失效，请重新登录");
            return false;
        }

        // 塞上下文，业务代码用 SecurityContext.getUserId() 取
        SecurityContext.setUserId(userId);
        SecurityContext.setRoleId(jwtUtil.getRoleId(token));
        SecurityContext.setRoleCode(jwtUtil.getRoleCode(token));
        SecurityContext.setToken(token);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求结束一定要清掉，不然线程复用时数据会串
        SecurityContext.clear();
    }

    /** 从请求头取 token，去掉 Bearer 前缀 */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            return null;
        }
        String prefix = "Bearer";
        if (header.startsWith(prefix + " ")) {
            return header.substring(prefix.length() + 1).trim();
        }
        return header.trim();
    }

    /** 直接往响应里写 401，不走 Controller 了 */
    private void writeUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(MAPPER.writeValueAsString(Result.unauthorized(msg)));
    }
}
