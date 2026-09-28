package com.qll.ucch.models.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文章类型字典实体，对应 blog_type。
 * 1项目 2需求 3课程，作为静态字典由管理员维护（一般不动）。
 *
 * @author qll
 */
@Data
@TableName("blog_type")
public class BlogType implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 类型ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 类型名称 */
    private String name;

    /** 类型描述 */
    private String description;
}
