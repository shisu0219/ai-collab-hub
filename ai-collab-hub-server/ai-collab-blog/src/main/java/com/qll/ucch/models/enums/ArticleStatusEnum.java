package com.qll.ucch.models.enums;

import lombok.Getter;

/**
 * 文章状态枚举，对应 blog_article_status 字典表。
 * 新发布的文章默认落在「待审核」，管理员审核通过后才变成「已发布」对外可见。
 *
 * @author qll
 */
@Getter
public enum ArticleStatusEnum {

    /** 待审核：刚提交，还没人处理 */
    WAIT_AUDIT(1L, "待审核"),

    /** 已发布：审核通过，列表里能看到 */
    PUBLISHED(2L, "已发布"),

    /** 已拒绝：审核没通过 */
    REJECTED(3L, "已拒绝"),

    /** 已下架：管理员或发布者主动下架 */
    OFFLINE(4L, "已下架");

    private final Long id;

    private final String name;

    ArticleStatusEnum(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static ArticleStatusEnum of(Long id) {
        if (id == null) {
            return null;
        }
        for (ArticleStatusEnum item : values()) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }
}
