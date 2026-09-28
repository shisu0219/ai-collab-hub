package com.qll.ucch.models.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 进度字典实体，对应 blog_progress。
 * 按文章类型挂在不同的进度节点上（比如项目类：招募中/进行中/已完成）。
 *
 * @author qll
 */
@Data
@TableName("blog_progress")
public class BlogProgress implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 进度ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 进度名称 */
    private String name;

    /** 关联文章类型ID */
    private Long typeId;

    /** 进度描述 */
    private String description;

    /** 节点数量 */
    private Integer nodeCount;
}
