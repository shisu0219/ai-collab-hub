package com.qll.ucch.exception;

import lombok.Getter;

/**
 * 未登录 / 登录失效异常。
 * 单独拎出来是因为前端要根据这个状态码跳登录页。
 *
 * @author 人工智能学院双创平台
 */
@Getter
public class UnauthorizedException extends RuntimeException {

    private final Integer code = 401;

    public UnauthorizedException() {
        super("登录已失效，请重新登录");
    }

    public UnauthorizedException(String message) {
        super(message);
    }
}
