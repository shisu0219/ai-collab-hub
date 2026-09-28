package com.qll.ucch.models;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结果。
 * 前端拿到的每一个接口响应都是这个壳子，方便统一处理。
 *
 * @author 人工智能学院双创平台
 */
@Data
@Schema(description = "统一返回结果")
public class Result<T> implements Serializable {

    /** 业务状态码：200 成功，其它为失败 */
    @Schema(description = "状态码，200 表示成功")
    private Integer code;

    /** 提示信息 */
    @Schema(description = "提示信息")
    private String msg;

    /** 业务数据 */
    @Schema(description = "业务数据")
    private T data;

    public Result() {
    }

    public Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /** 成功，不带数据 */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    /** 成功，带数据 */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /** 成功，自定义提示语 */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(200, msg, data);
    }

    /** 失败，默认 500 */
    public static <T> Result<T> fail(String msg) {
        return new Result<>(500, msg, null);
    }

    /** 失败，自定义状态码 */
    public static <T> Result<T> fail(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    /** 参数校验不通过常用 */
    public static <T> Result<T> badRequest(String msg) {
        return new Result<>(400, msg, null);
    }

    /** 未登录或登录失效 */
    public static <T> Result<T> unauthorized(String msg) {
        return new Result<>(401, msg, null);
    }

    /** 没有权限 */
    public static <T> Result<T> forbidden(String msg) {
        return new Result<>(403, msg, null);
    }
}
