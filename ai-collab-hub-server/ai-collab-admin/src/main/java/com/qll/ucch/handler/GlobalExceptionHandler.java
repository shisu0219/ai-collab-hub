package com.qll.ucch.handler;

import com.qll.ucch.exception.BusinessException;
import com.qll.ucch.exception.UnauthorizedException;
import com.qll.ucch.models.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理。
 * 有了它 Controller 里就不用到处 try-catch，抛出去统一转成 Result 返回。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：自己主动抛的，返回给前端看得懂的提示 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 [{}] {}", request.getRequestURI(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * blog 模块自带的 BusinessException。
     *
     * 【为什么要单独接一个】
     * 项目里有两个同名的 BusinessException：
     *   com.qll.ucch.exception.BusinessException        （common 模块）
     *   com.qll.ucch.models.common.BusinessException    （blog 模块自带）
     * blog 模块为了不把编译绑死在别的模块上，自己带了一个最简版，
     * 但它的注释里写着「交给 admin 层的全局异常处理器兜底」—— 而处理器之前只认上面那个。
     *
     * 结果：blog 模块抛的业务提示（「请填写你的年级和班级」这类）全部掉进兜底分支，
     * 前端统一显示成「服务器开小差了，请稍后再试」，用户看不到真正的错误原因。
     *
     * 两个类结构一致（都继承 RuntimeException + 有 getCode()），所以处理方式一样。
     */
    @ExceptionHandler(com.qll.ucch.models.common.BusinessException.class)
    public Result<Void> handleBlogBusiness(com.qll.ucch.models.common.BusinessException e,
                                           HttpServletRequest request) {
        log.warn("业务异常 [{}] {}", request.getRequestURI(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 未登录异常 */
    @ExceptionHandler(UnauthorizedException.class)
    public Result<Void> handleUnauthorized(UnauthorizedException e) {
        return Result.unauthorized(e.getMessage());
    }

    /** 参数校验异常：@Valid 校验不通过时抛这个 */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidException(Exception e) {
        String msg = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException ex) {
            FieldError fieldError = ex.getBindingResult().getFieldError();
            if (fieldError != null) {
                msg = fieldError.getDefaultMessage();
            }
        } else if (e instanceof BindException ex) {
            FieldError fieldError = ex.getBindingResult().getFieldError();
            if (fieldError != null) {
                msg = fieldError.getDefaultMessage();
            }
        }
        return Result.badRequest(msg);
    }

    /** 上传文件超过大小限制 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxSize(MaxUploadSizeExceededException e) {
        log.warn("上传文件过大：{}", e.getMessage());
        return Result.fail("上传的文件太大了，单个文件不能超过 100MB");
    }

    /** 兜底：其它没预料到的异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 [{}]", request.getRequestURI(), e);
        return Result.fail("服务器开小差了，请稍后再试");
    }
}
