package com.qll.ucch.models.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 需求类扩展实体，对应 blog_demand_extend。
 * 学生发布用人/合作需求时填紧急程度和期望完成日期。
 *
 * @author qll
 */
@Data
@TableName("blog_demand_extend")
public class BlogDemandExtend implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 文章ID */
    private Long articleId;

    /** 紧急程度 */
    private String urgencyLevel;

    /** 期望完成日期 */
    private LocalDate expectedDeadline;
}
