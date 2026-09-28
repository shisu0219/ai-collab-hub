package com.qll.ucch.constant;

/**
 * 系统模块用到的常量。
 * 角色 id 和 sql 初始化脚本里写死的一致，改的时候两边要一起改。
 */
public final class SysConstants {

    private SysConstants() {
    }

    /** 管理员角色ID */
    public static final Long ROLE_ADMIN_ID = 1L;

    /** 学生角色ID */
    public static final Long ROLE_STUDENT_ID = 2L;

    /** 老师角色ID */
    public static final Long ROLE_TEACHER_ID = 4L;

    /** 角色标识 */
    public static final String ROLE_CODE_ADMIN = "ADMIN";
    public static final String ROLE_CODE_STUDENT = "STUDENT";
    public static final String ROLE_CODE_TEACHER = "TEACHER";

    /** 审核状态：待审核 */
    public static final Integer AUDIT_PENDING = 0;

    /** 审核状态：通过 */
    public static final Integer AUDIT_PASSED = 1;

    /** 审核状态：拒绝 */
    public static final Integer AUDIT_REJECTED = 2;

    /** 启用 */
    public static final Integer ENABLE_YES = 1;

    /** 禁用 */
    public static final Integer ENABLE_NO = 0;

    /**
     * 密码强度：至少 8 位，且必须同时包含大写字母、小写字母、数字、特殊字符。
     * 用环视做的整体校验，顺序不限（比如 Abc@1234、1aB@cdef 都行）。
     */
    public static final String PASSWORD_REGEX =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,64}$";

    /** 密码强度提示语，注册和重置密码共用 */
    public static final String PASSWORD_TIP =
            "密码至少8位，且必须同时包含大写字母、小写字母、数字和特殊字符";

    /** 账号格式：字母、数字、下划线，至少 4 位 */
    public static final String ACCOUNT_REGEX = "^[A-Za-z0-9_]{4,64}$";

    /** 账号格式提示语 */
    public static final String ACCOUNT_TIP = "账号至少4位，只能用字母、数字、下划线";
}
