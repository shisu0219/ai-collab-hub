package com.qll.ucch.models.enums;

import lombok.Getter;

/**
 * 审核状态枚举。
 * 数值和数据库里 sys_user.audit_status、sys_user_review.audit_status 对应。
 *
 * @author 人工智能学院双创平台
 */
@Getter
public enum AuditStatusEnum {

    PENDING(0, "待审核"),
    PASS(1, "已通过"),
    REJECT(2, "已拒绝");

    private final Integer code;
    private final String desc;

    AuditStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String descOf(Integer code) {
        if (code == null) {
            return "";
        }
        for (AuditStatusEnum e : values()) {
            if (e.code.equals(code)) {
                return e.desc;
            }
        }
        return "";
    }
}
