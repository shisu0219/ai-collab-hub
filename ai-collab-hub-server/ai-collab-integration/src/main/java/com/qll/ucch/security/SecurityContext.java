package com.qll.ucch.security;

/**
 * 登录用户上下文。
 * <p>
 * 拦截器解析完 JWT 之后会把当前用户信息塞进来，业务层（Service）直接从
 * 这里取，省得每个方法都多带一个 userId 参数。
 * <p>
 * 注意两点：
 * 1. 底层用 ThreadLocal，用完必须 clear()，否则线程池复用时会串号，所以
 *    拦截器一定得在 afterCompletion 里调 clear()。
 * 2. 异步线程 / 定时任务里拿不到值，需要自己往下传。
 *
 * @author 人工智能学院双创平台
 */
public class SecurityContext {

    /** 当前线程绑定的登录信息 */
    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private SecurityContext() {
        // 工具类，不允许实例化
    }

    /**
     * 设置当前登录用户（简版，只有用户ID和角色标识）
     */
    public static void set(Long userId, String roleCode) {
        HOLDER.set(new LoginUser(userId, roleCode));
    }

    /**
     * 设置完整的登录用户对象
     */
    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    /** 设置当前登录用户ID */
    public static void setUserId(Long userId) {
        LoginUser user = HOLDER.get();
        if (user == null) {
            user = new LoginUser();
            HOLDER.set(user);
        }
        user.setUserId(userId);
    }

    /** 设置当前登录用户角色ID */
    public static void setRoleId(Long roleId) {
        LoginUser user = HOLDER.get();
        if (user == null) {
            user = new LoginUser();
            HOLDER.set(user);
        }
        user.setRoleId(roleId);
    }

    /** 设置当前登录用户角色标识 */
    public static void setRoleCode(String roleCode) {
        LoginUser user = HOLDER.get();
        if (user == null) {
            user = new LoginUser();
            HOLDER.set(user);
        }
        user.setRoleCode(roleCode);
    }

    /** 设置当前请求携带的 token */
    public static void setToken(String token) {
        LoginUser user = HOLDER.get();
        if (user == null) {
            user = new LoginUser();
            HOLDER.set(user);
        }
        user.setToken(token);
    }

    /**
     * 取当前登录用户对象，未登录返回 null
     */
    public static LoginUser get() {
        return HOLDER.get();
    }

    /**
     * 当前登录用户ID，未登录返回 null
     */
    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    /**
     * 当前登录用户ID，未登录直接抛异常。
     * 用在「必须是登录态」的业务里，避免到处判空。
     */
    public static Long requireUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw new IllegalStateException("当前用户未登录，无法获取用户信息");
        }
        return userId;
    }

    /** 当前登录用户角色ID，未登录返回 null */
    public static Long getRoleId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getRoleId();
    }

    /**
     * 当前登录用户角色标识，未登录返回 null
     */
    public static String getRoleCode() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getRoleCode();
    }

    /** 当前请求的 token */
    public static String getToken() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getToken();
    }

    /**
     * 是否管理员
     */
    public static boolean isAdmin() {
        return "ADMIN".equals(getRoleCode());
    }

    /**
     * 清理，务必在请求结束（afterCompletion）时调用
     */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 登录用户信息载体
     */
    public static class LoginUser {

        /** 用户ID */
        private Long userId;

        /** 角色ID */
        private Long roleId;

        /** 角色标识（STUDENT / TEACHER / ADMIN） */
        private String roleCode;

        /** 本次请求的 token */
        private String token;

        public LoginUser() {
        }

        public LoginUser(Long userId, String roleCode) {
            this.userId = userId;
            this.roleCode = roleCode;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getRoleId() {
            return roleId;
        }

        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }

        public String getRoleCode() {
            return roleCode;
        }

        public void setRoleCode(String roleCode) {
            this.roleCode = roleCode;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        @Override
        public String toString() {
            return "LoginUser{userId=" + userId + ", roleId=" + roleId
                    + ", roleCode='" + roleCode + "'}";
        }
    }
}
