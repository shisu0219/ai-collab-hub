package com.qll.ucch.models.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章审核记录实体，对应 blog_article_review。
 * 每次审核都留一条流水，文章主表的 status_id 同步更新，方便日后追溯是谁审的。
 *
 * @author qll
 */
@Data
@TableName("blog_article_review")
public class BlogArticleReview implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 审核记录ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 文章ID */
    private Long articleId;

    /** 审核人ID */
    private Long reviewerId;

    /** 是否通过：1通过 0拒绝 */
    private Integer pass;

    /** 审核意见 */
    private String reason;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
