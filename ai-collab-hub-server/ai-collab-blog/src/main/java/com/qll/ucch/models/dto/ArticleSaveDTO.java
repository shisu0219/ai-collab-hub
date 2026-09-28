package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 文章发布/修改入参。
 * 三类内容共用一个 DTO，扩展字段按 typeId 走不同的分组：
 * - typeId=1 项目：budget / startDate / endDate
 * - typeId=2 需求：urgencyLevel / expectedDeadline
 * - typeId=3 课程：maxStudents / lessonTime / locationDetail
 *
 * @author qll
 */
@Data
@Schema(description = "文章发布/修改入参")
public class ArticleSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "文章ID，修改时必传；新增不传")
    private Long id;

    @Schema(description = "发布者用户ID（由 Controller 从登录态取，Service 内部也会兜底校验）")
    private Long userId;

    @Schema(description = "标题")
    @NotBlank(message = "标题不能为空")
    private String title;

    @Schema(description = "类型ID：1项目 2需求 3课程")
    @NotNull(message = "请选择内容类型")
    private Long typeId;

    @Schema(description = "详细描述")
    private String content;

    @Schema(description = "地区/地点")
    private String location;

    @Schema(description = "所属小组ID（学生填写时必填，老师发项目想法可不填）")
    private Long groupId;

    @Schema(description = "视频访问地址（下一轮启用）")
    private String videoUrl;

    @Schema(description = "视频封面图（下一轮启用）")
    private String videoCover;

    @Schema(description = "标签ID")
    private Long tagId;

    @Schema(description = "进度ID")
    private Long progressId;

    @Schema(description = "附件地址列表，逗号分隔")
    private String attachments;

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

    @Schema(description = "上课时间")
    private String lessonTime;

    @Schema(description = "详细地点")
    private String locationDetail;
}
