package com.qll.ucch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域（CORS）配置。
 *
 * 【为什么需要这个】
 * 本地开发时前端通过 Vite 代理访问后端（同源），不需要跨域配置。
 * 但把前端部署到 GitHub Pages 之后，前端在 github.io、后端在别的域名/IP ——
 * 变成跨域请求，浏览器默认会拦截，表现为「接口全部失败」。
 *
 * 【安全说明】
 * 这里允许所有来源（allowedOriginPatterns("*")），方便部署到任何地方都能跑。
 * 因为鉴权走的是 JWT（token 在请求头里），不靠 Cookie，
 * 所以开 CORS 不会带来 CSRF 风险。
 * 如果以后要收紧，把 allowedOriginPatterns 换成具体域名即可，比如：
 *   .allowedOriginPatterns("https://shisu0219.github.io", "http://localhost:3000")
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 允许的来源。用 pattern 是为了兼容带端口的 localhost 等场景
                .allowedOriginPatterns("*")
                // 允许的请求方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                // 允许所有请求头（token 放在 Authorization 里，必须放行）
                .allowedHeaders("*")
                // 允许前端读取的响应头（文件下载要读 Content-Disposition）
                .exposedHeaders("Content-Disposition")
                // 是否允许带凭证。走 JWT 不需要，这里关掉更安全
                .allowCredentials(false)
                // 预检请求缓存时间（秒），减少 OPTIONS 请求次数
                .maxAge(3600);
    }
}
