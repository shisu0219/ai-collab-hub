package com.qll.ucch.security;

import java.lang.annotation.*;

/**
 * 标记不需要登录就能访问的接口。
 * 可以打在方法上，也可以打在 Controller 类上（整个类都免登录）。
 *
 * @author 人工智能学院双创平台
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreAuth {
}
