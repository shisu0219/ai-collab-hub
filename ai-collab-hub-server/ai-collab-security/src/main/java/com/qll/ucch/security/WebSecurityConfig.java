package com.qll.ucch.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：把登录拦截器挂上去。
 * 排除掉登录、注册、找回密码这些免登录接口，其余统一走拦截器。
 *
 * @author 人工智能学院双创平台
 */
@Configuration
@RequiredArgsConstructor
public class WebSecurityConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    /** 不需要登录就能访问的路径 */
    private static final String[] WHITE_LIST = {
            // 登录、注册、找回密码
            "/user/login/**",
            "/user/register/**",
            "/user/find/pwd/**",
            // 接口文档
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-resources/**",
            // 错误页
            "/error",
            // 上传文件的静态访问
            "/WebFile/**",
            // 公开浏览内容（未登录也能看）
            "/blog/article/list/brief",
            "/blog/article/static/**"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(WHITE_LIST);
    }
}
