package com.qll.ucch.models.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文章状态字典实体，对应 blog_article_status。
 * 1待审核 2已发布 3已拒绝 4已下架。
 *
 * @author qll
 */
@Data
@TableName("blog_article_status")
public class BlogArticleStatus implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 状态ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 状态名称 */
    private String name;

    /** 状态描述 */
    private String description;
}
