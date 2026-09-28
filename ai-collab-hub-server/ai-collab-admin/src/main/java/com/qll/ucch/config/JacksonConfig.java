package com.qll.ucch.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 全局配置。
 * <p>
 * 核心是把 Long 类型统一序列化成字符串。
 * <p>
 * 原因：主键用的是雪花算法生成的 Long（比如 2101727709056671745），
 * 这个数超过了 JS 的安全整数上限（Number.MAX_SAFE_INTEGER = 9007199254740991），
 * 浏览器 JSON.parse 之后末位会变成 4 或者 6 —— 精度丢了，
 * 前端再拿这个错 ID 去查详情就会查不到，报「会话不存在」「内容不存在」这类莫名其妙的错。
 * <p>
 * 转成字符串之后前端原样传回来就没问题了。前端拿到的 id 是字符串，
 * 做比较时记得用 String() 转一下，别直接用 == 比数字。
 *
 * @author 人工智能学院双创平台
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule("longToString");
            // Long 和 long 都转字符串
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            // 注意用 modulesToInstall 而不是 modules：
            // modules() 是覆盖整个模块列表，会把 Spring Boot 默认注册的
            // JavaTimeModule 也顶掉，导致 LocalDateTime 序列化直接报错。
            builder.modulesToInstall(module);
        };
    }
}
