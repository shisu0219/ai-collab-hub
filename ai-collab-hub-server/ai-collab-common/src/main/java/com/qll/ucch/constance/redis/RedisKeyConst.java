package com.qll.ucch.constance.redis;

/**
 * Redis Key 命名规范。
 * 统一前缀，避免跟服务器上其它项目的 key 撞车。
 *
 * @author 人工智能学院双创平台
 */
public interface RedisKeyConst {

    /** 项目统一前缀 */
    String PREFIX = "aichub:";

    /** 登录 token 前缀 */
    String LOGIN_TOKEN_PREFIX = "aichub:token:";

    /** 登录用户信息缓存前缀 */
    String LOGIN_USER_PREFIX = "aichub:login:user:";

    /** 验证码前缀 */
    String CAPTCHA_PREFIX = "aichub:captcha:";

    /** 找回密码验证码前缀 */
    String FIND_PWD_PREFIX = "aichub:findpwd:";

    /** 内容浏览量计数前缀 */
    String ARTICLE_VIEW_PREFIX = "aichub:view:";

    /** 接口限流前缀 */
    String RATE_LIMIT_PREFIX = "aichub:ratelimit:";

    /** token 有效期，7 天，单位秒 */
    long TOKEN_EXPIRE_SECONDS = 604800L;
}
