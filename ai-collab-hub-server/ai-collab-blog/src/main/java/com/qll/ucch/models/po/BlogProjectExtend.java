package com.qll.ucch.models.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 项目类扩展实体，对应 blog_project_extend。
 * 学生发布项目时填的预算和起止日期，一篇文章一条，article_id 有唯一索引。
 *
 * @author qll
 */
@Data
@TableName("blog_project_extend")
public class BlogProjectExtend implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 文章ID */
    private Long articleId;

    /** 项目预算 */
    private String budget;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;
}
