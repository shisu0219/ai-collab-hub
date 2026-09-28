package com.qll.ucch.models.common;

import lombok.Getter;

/**
 * 业务异常。
 * 本模块只依赖 ai-collab-common / integration / notice 三个模块，且它们由其他人并行产出，
 * 为了不把编译绑死在别处的类上，这里自带一个最简的运行时异常，交给 admin 层的全局异常处理器兜底。
 *
 * @author qll
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 错误码，默认 500 */
    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }
}
