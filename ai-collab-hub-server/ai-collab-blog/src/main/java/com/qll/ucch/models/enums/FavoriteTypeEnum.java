package com.qll.ucch.models.enums;

import lombok.Getter;

/**
 * 收藏对象类型枚举（对应 user_favorite.target_type）。
 * 目前只做「收藏文章」和「关注用户」两类。
 *
 * @author qll
 */
@Getter
public enum FavoriteTypeEnum {

    /** 文章：收藏别人的合作内容 */
    ARTICLE(1, "文章"),

    /** 用户：关注某个人 */
    USER(2, "用户");

    private final Integer code;

    private final String name;

    FavoriteTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static FavoriteTypeEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (FavoriteTypeEnum item : values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }
}
