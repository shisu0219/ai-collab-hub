package com.qll.ucch.exception;

/**
 * 系统模块的业务异常。
 * <p>
 * 继承自 common 模块的 {@link BusinessException}，这样全局异常处理器
 * （GlobalExceptionHandler）能直接把提示语返回给前端，不用额外再注册一个 handler。
 * <p>
 * 之所以还单独留这个类，是为了让 ai-collab-sys 模块的语义更清楚：
 * 看到 SysBizException 就知道是用户/角色这块抛出来的。
 *
 * @author 人工智能学院双创平台
 */
public class SysBizException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public SysBizException(String message) {
        super(message);
    }

    public SysBizException(Integer code, String message) {
        super(code, message);
    }
}
