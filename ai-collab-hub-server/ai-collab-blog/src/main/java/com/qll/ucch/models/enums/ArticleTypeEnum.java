package com.qll.ucch.models.enums;

import lombok.Getter;

/**
 * 文章类型枚举，对应 blog_type 字典表的初始化数据。
 * 项目：学生发布的合作项目；需求：学生发布的用人/合作需求；课程：培训课程类内容。
 *
 * @author qll
 */
@Getter
public enum ArticleTypeEnum {

    /** 项目：学生发布 */
    PROJECT(1L, "项目"),

    /** 需求：学生发布 */
    DEMAND(2L, "需求"),

    /** 课程：培训课程 */
    LESSON(3L, "课程");

    private final Long id;

    private final String name;

    ArticleTypeEnum(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * 根据类型ID反查枚举，查不到返回 null，调用方自行兜底。
     */
    public static ArticleTypeEnum of(Long id) {
        if (id == null) {
            return null;
        }
        for (ArticleTypeEnum item : values()) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }
}
