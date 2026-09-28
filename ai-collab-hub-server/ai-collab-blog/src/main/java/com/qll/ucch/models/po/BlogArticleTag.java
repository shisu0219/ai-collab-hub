package com.qll.ucch.models.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文章标签实体，对应 blog_article_tag。
 * 标签是全局共用的字典，tag_name 在库里有唯一索引。
 *
 * @author qll
 */
@Data
@TableName("blog_article_tag")
public class BlogArticleTag implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 标签ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 标签名称 */
    private String tagName;
}
