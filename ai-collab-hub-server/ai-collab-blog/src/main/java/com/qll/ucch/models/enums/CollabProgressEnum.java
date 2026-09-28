package com.qll.ucch.models.enums;

import lombok.Getter;

/**
 * 对接进度枚举（对应 blog_registration.collab_progress 字段）。
 * 这是新增的进度流转能力：申请提交后从「待处理」出发，一路推进到洽谈 / 合作 / 结束。
 *
 * @author qll
 */
@Getter
public enum CollabProgressEnum {

    /** 待处理：刚提交申请，对方还没看 */
    WAIT(0, "待处理"),

    /** 已通过：对方同意了对接申请 */
    PASSED(1, "已通过"),

    /** 洽谈中：双方已经开始谈细节 */
    TALKING(2, "洽谈中"),

    /** 已合作：达成合作 */
    COOPERATED(3, "已合作"),

    /** 已结束：合作终止或项目收尾 */
    FINISHED(4, "已结束"),

    /** 已拒绝：对方谢绝了申请 */
    REFUSED(5, "已拒绝");

    private final Integer code;

    private final String name;

    CollabProgressEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static CollabProgressEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (CollabProgressEnum item : values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 校验进度值是否是合法状态，Controller / Service 入参先过一遍，避免脏数据。
     */
    public static boolean isValid(Integer code) {
        return of(code) != null;
    }

    /**
     * 简单的状态流转校验：只允许「往前走」或者被拒绝，避免进度被随意回退。
     * 待处理 -> 已通过/已拒绝；已通过 -> 洽谈中/已结束；洽谈中 -> 已合作/已结束；
     * 已合作 -> 已结束；已结束/已拒绝 属于终态，不允许再改。
     */
    public boolean canTransferTo(Integer target) {
        CollabProgressEnum targetEnum = of(target);
        if (targetEnum == null) {
            return false;
        }
        if (this == targetEnum) {
            return true;
        }
        return switch (this) {
            case WAIT -> targetEnum == PASSED || targetEnum == REFUSED;
            case PASSED -> targetEnum == TALKING || targetEnum == REFUSED || targetEnum == FINISHED;
            case TALKING -> targetEnum == COOPERATED || targetEnum == FINISHED;
            case COOPERATED -> targetEnum == FINISHED;
            case FINISHED, REFUSED -> false;
        };
    }
}
