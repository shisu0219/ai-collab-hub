package com.qll.ucch.handler;

import com.qll.ucch.security.SecurityContext;
import com.qll.ucch.service.SysOperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志切面（新增功能）。
 * <p>
 * 自动给管理员的关键写操作记一笔日志，不用在业务代码里到处插 record() 调用。
 * <p>
 * 记录范围：审核（用户/内容）、启用禁用用户、增删标签、下架内容。
 * 这些都是「改了平台数据、出了问题要追责」的动作，其它的不记，免得日志表爆掉。
 * <p>
 * 拿不到当前用户（比如定时任务触发）时跳过，不写脏数据。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SysOperationLogService operationLogService;

    /** 用户审核：通过 / 拒绝 / 批量 */
    @Pointcut("execution(* com.qll.ucch.openapi.user.UserReviewController.*(..))")
    public void userReviewPointcut() {
    }

    /** 内容审核：通过 / 拒绝 / 批量 */
    @Pointcut("execution(* com.qll.ucch.openapi.blog.BlogReviewController.*(..))")
    public void articleReviewPointcut() {
    }

    /** 账户管理：启用禁用 */
    @Pointcut("execution(* com.qll.ucch.openapi.user.UserRootController.*(..))")
    public void userRootPointcut() {
    }

    /** 标签管理：增删 */
    @Pointcut("execution(* com.qll.ucch.openapi.blog.BlogRootController.*(..))")
    public void blogRootPointcut() {
    }

    @Around("userReviewPointcut() || articleReviewPointcut() "
            + "|| userRootPointcut() || blogRootPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Object result = point.proceed();

        // 业务执行完再记录，出异常的话不记（异常本身有全局日志）
        try {
            writeLog(point);
        } catch (Exception e) {
            log.warn("切面写操作日志失败：{}", e.getMessage());
        }

        return result;
    }

    private void writeLog(ProceedingJoinPoint point) {
        Long userId = SecurityContext.getUserId();
        if (userId == null) {
            // 没有登录态（比如内部调用），不记
            return;
        }

        String methodName = point.getSignature().getName();
        String className = point.getTarget().getClass().getSimpleName();

        String module = resolveModule(className);
        String action = resolveAction(className, methodName);

        operationLogService.record(
                userId,
                // 账号不在登录上下文里，留空由日志查询时按 userId 关联出来
                null,
                module,
                action,
                null,
                buildDetail(className, methodName, point.getArgs()),
                currentIp()
        );
    }

    /** 类名 → 中文模块名 */
    private String resolveModule(String className) {
        if (className.contains("UserReview")) {
            return "用户审核";
        }
        if (className.contains("BlogReview")) {
            return "内容审核";
        }
        if (className.contains("UserRoot") || className.contains("AccountManage")) {
            return "账户管理";
        }
        if (className.contains("BlogRoot")) {
            return "标签管理";
        }
        return "系统管理";
    }

    /** 方法名 → 中文动作 */
    private String resolveAction(String className, String methodName) {
        String lower = methodName.toLowerCase();
        if (lower.contains("batch")) {
            return "批量操作";
        }
        if (lower.contains("approve") || lower.contains("pass")) {
            return "审核通过";
        }
        if (lower.contains("reject")) {
            return "审核拒绝";
        }
        if (lower.contains("enable") || lower.contains("disable")) {
            return "启用禁用账号";
        }
        if (lower.contains("add") || lower.contains("save") || lower.contains("create")) {
            return "新增";
        }
        if (lower.contains("delete") || lower.contains("remove")) {
            return "删除";
        }
        return methodName;
    }

    /** 参数摘要，截断避免超长 */
    private String buildDetail(String className, String methodName, Object[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append(className).append(".").append(methodName);
        if (args != null && args.length > 0) {
            sb.append(" 参数:");
            for (Object a : args) {
                if (a == null) {
                    continue;
                }
                String s = String.valueOf(a);
                // 太长的对象只留前 200 字
                sb.append(s.length() > 200 ? s.substring(0, 200) + "..." : s).append(";");
            }
        }
        String detail = sb.toString();
        return detail.length() > 900 ? detail.substring(0, 900) + "..." : detail;
    }

    /** 从请求上下文取 IP */
    private String currentIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
