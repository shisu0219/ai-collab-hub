package com.qll.ucch.models.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 文章详情出参。
 * 主表字段 + 按类型填充的扩展字段 + 发布者信息占位（跨模块的用户信息由 admin 层补全，这里先留字段）。
 *
 * @author qll
 */
@Data
@Schema(description = "文章详情")
public class ArticleDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // ---------------- 主表字段 ----------------

    @Schema(description = "文章ID")
    private Long id;

    @Schema(description = "发布者用户ID")
    private Long userId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "类型ID")
    private Long typeId;

    @Schema(description = "类型名称")
    private String typeName;

    @Schema(description = "详细描述")
    private String content;

    @Schema(description = "地区/地点")
    private String location;

    @Schema(description = "标签ID")
    private Long tagId;

    @Schema(description = "标签名称")
    private String tagName;

    @Schema(description = "状态ID")
    private Long statusId;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "进度ID")
    private Long progressId;

    @Schema(description = "进度名称")
    private String progressName;

    @Schema(description = "附件地址列表，逗号分隔")
    private String attachments;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "发布时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // ---------------- 发布者信息占位 ----------------

    @Schema(description = "发布者昵称（由上层聚合填充，本模块不查用户表）")
    private String publisherName;

    @Schema(description = "发布者头像")
    private String publisherAvatar;

    /** 发布者角色码：STUDENT / TEACHER / ADMIN，前端据此显示「学生」「老师」等 */
    private String publisherRoleCode;

    /** 发布者角色中文名 */
    private String publisherRoleName;

    @Schema(description = "当前用户是否已收藏该文章")
    private Boolean favorited;

    // ---------------- 项目类扩展 ----------------

    @Schema(description = "项目预算")
    private String budget;

    @Schema(description = "项目开始日期")
    private LocalDate startDate;

    @Schema(description = "项目结束日期")
    private LocalDate endDate;

    // ---------------- 需求类扩展 ----------------

    @Schema(description = "紧急程度")
    private String urgencyLevel;

    @Schema(description = "期望完成日期")
    private LocalDate expectedDeadline;

    // ---------------- 课程类扩展 ----------------

    @Schema(description = "人数上限")
    private Integer maxStudents;

    @Schema(description = "当前人数")
    private Integer currentStudents;

    @Schema(description = "上课时间")
    private String lessonTime;

    @Schema(description = "详细地点")
    private String locationDetail;
}
