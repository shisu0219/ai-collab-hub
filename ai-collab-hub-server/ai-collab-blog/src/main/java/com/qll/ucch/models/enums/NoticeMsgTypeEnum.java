package com.qll.ucch.models.enums;

import lombok.Getter;

/**
 * 站内信分类枚举，与 notice_box_info.msg_type 字段保持一致。
 * 本模块发通知时统一用这里的值，避免各处写魔法数字。
 *
 * @author qll
 */
@Getter
public enum NoticeMsgTypeEnum {

    /** 审核结果：内容审核通过/拒绝 */
    AUDIT_RESULT(1, "审核结果"),

    /** 收到申请：有人申请对接你的内容 */
    RECEIVE_REGISTRATION(2, "收到申请"),

    /** 系统通知：默认兜底 */
    SYSTEM(3, "系统通知"),

    /** 会话消息：对接通过后的双方聊天提醒 */
    CHAT(4, "会话消息");

    private final Integer code;

    private final String name;

    NoticeMsgTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static NoticeMsgTypeEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (NoticeMsgTypeEnum item : values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }
}
