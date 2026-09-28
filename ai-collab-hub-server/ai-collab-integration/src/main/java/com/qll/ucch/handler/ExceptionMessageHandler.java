package com.qll.ucch.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 异常信息提取辅助。
 * <p>
 * 全局异常处理器（放在 ai-collab-admin）里处理参数校验异常时，经常要把一堆
 * FieldError 拼成人能看懂的提示，这段逻辑抽出来放这，避免异常处理器里写一堆
 * for 循环。
 *
 * @author qll
 */
public class ExceptionMessageHandler {

    private ExceptionMessageHandler() {
    }

    /**
     * 从 @RequestBody 参数校验异常里取提示
     */
    public static String extractMessage(MethodArgumentNotValidException e) {
        if (e == null || e.getBindingResult() == null) {
            return "参数校验失败";
        }
        List<FieldError> errors = e.getBindingResult().getFieldErrors();
        if (errors.isEmpty()) {
            return "参数校验失败";
        }
        return join(errors.stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList());
    }

    /**
     * 从表单绑定异常里取提示
     */
    public static String extractMessage(BindException e) {
        if (e == null || e.getBindingResult() == null) {
            return "参数绑定失败";
        }
        List<FieldError> errors = e.getBindingResult().getFieldErrors();
        if (errors.isEmpty()) {
            return "参数绑定失败";
        }
        return join(errors.stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList());
    }

    /**
     * 从方法级校验异常里取提示（@Validated 打在 Service/Controller 方法参数上）
     */
    public static String extractMessage(ConstraintViolationException e) {
        if (e == null || e.getConstraintViolations() == null) {
            return "参数校验失败";
        }
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        if (violations.isEmpty()) {
            return "参数校验失败";
        }
        List<String> messages = new ArrayList<>(violations.size());
        for (ConstraintViolation<?> violation : violations) {
            messages.add(violation.getPropertyPath() + ": " + violation.getMessage());
        }
        return join(messages);
    }

    /**
     * 上传类异常提示：把底层信息包一层，前端能直接弹
     */
    public static String uploadErrorTip(Exception e) {
        if (e == null) {
            return "文件上传失败";
        }
        if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
            return e.getMessage();
        }
        if (e.getMessage() != null && e.getMessage().contains("Maximum upload size")) {
            return "上传文件过大，整体附件上限 1024MB，单个附件上限 100MB";
        }
        return "文件上传失败：" + e.getMessage();
    }

    /**
     * 最多拼 3 条，多了看不过来
     */
    private static String join(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return "参数校验失败";
        }
        int limit = Math.min(messages.size(), 3);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                sb.append("；");
            }
            sb.append(messages.get(i));
        }
        if (messages.size() > limit) {
            sb.append(" 等 ").append(messages.size()).append(" 项");
        }
        return sb.toString();
    }
}
