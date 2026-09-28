package com.qll.ucch.models.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对接申请出参。
 * 「我收到的」列表里申请人信息、「我发出的」列表里文章信息，都由上层补充，这里以 ID + 标题兜底。
 *
 * @author qll
 */
@Data
@Schema(description = "对接申请信息")
public class RegistrationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "申请ID")
    private Long id;

    @Schema(description = "目标文章ID")
    private Long articleId;

    @Schema(description = "目标文章标题")
    private String articleTitle;

    @Schema(description = "文章类型ID")
    private Long typeId;

    @Schema(description = "文章类型名称")
    private String typeName;

    @Schema(description = "申请人用户ID")
    private Long userId;

    @Schema(description = "申请人昵称（占位，上层填充）")
    private String applicantName;

    @Schema(description = "文章发布者用户ID（我收到的列表里用来区分）")
    private Long articleOwnerId;

    @Schema(description = "年级")
    private String grade;

    @Schema(description = "班级")
    private String className;

    @Schema(description = "擅长部分（逗号分隔）")
    private String skills;

    @Schema(description = "擅长部分列表，前端直接渲染标签")
    private java.util.List<String> skillList;

    @Schema(description = "补充说明")
    private String reason;

    @Schema(description = "联系方式类型")
    private String contactWay;

    @Schema(description = "联系方式值")
    private String contactWayValue;

    @Schema(description = "附件地址列表")
    private String attachments;

    @Schema(description = "处理结果：1通过 0拒绝 null待处理")
    private Integer pass;

    @Schema(description = "回复内容")
    private String reviewMessage;

    @Schema(description = "对接进度：0待处理 1已通过 2洽谈中 3已合作 4已结束 5已拒绝")
    private Integer collabProgress;

    @Schema(description = "对接进度名称")
    private String collabProgressName;

    @Schema(description = "申请时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
