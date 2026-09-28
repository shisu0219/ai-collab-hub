package com.qll.ucch.exception;

import lombok.Getter;

/**
 * 业务异常。
 * service 层遇到不合法的操作直接抛这个，全局异常处理器会转成统一返回结构。
 *
 * @author 人工智能学院双创平台
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务状态码，默认 500 */
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
