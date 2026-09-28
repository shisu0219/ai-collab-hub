package com.qll.ucch.models.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章主表实体，对应 blog_article_info。
 * 项目 / 需求 / 课程 三类内容都存这一张主表，差异字段放到各自的扩展表里。
 *
 * @author qll
 */
@Data
@TableName("blog_article_info")
public class BlogArticleInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 文章ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 发布者用户ID */
    private Long userId;

    /** 所属小组ID（一个小组只能有一个项目） */
    private Long groupId;

    /** 标题 */
    private String title;

    /** 类型ID（1项目 2需求 3课程） */
    private Long typeId;

    /** 详细描述 */
    private String content;

    /** 地区/地点 */
    private String location;

    /** 标签ID */
    private Long tagId;

    /** 状态ID，默认 1 待审核 */
    private Long statusId;

    /** 进度ID */
    private Long progressId;

    /** 附件地址列表（逗号分隔） */
    private String attachments;

    /** 视频访问地址（下一轮启用，字段已预留） */
    private String videoUrl;

    /** 视频封面图（下一轮启用，字段已预留） */
    private String videoCover;

    /** 浏览量（新增字段：进详情页 +1） */
    private Integer viewCount;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 删除时间，非空表示已逻辑删除 */
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deleteTime;

    /** 创建人ID */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /** 更新人ID */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}
