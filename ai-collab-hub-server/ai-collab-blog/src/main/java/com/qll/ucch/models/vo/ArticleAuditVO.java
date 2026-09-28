package com.qll.ucch.models.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内容审核列表项。
 * 管理员审核台用，比普通列表多带发布者和审核状态上下文。
 *
 * @author qll
 */
@Data
@Schema(description = "待审核/审核记录项")
public class ArticleAuditVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "审核记录ID（没有审核记录时为 null）")
    private Long reviewId;

    @Schema(description = "文章ID")
    private Long articleId;

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

    @Schema(description = "附件地址列表")
    private String attachments;

    @Schema(description = "发布者用户ID")
    private Long userId;

    @Schema(description = "审核人ID")
    private Long reviewerId;

    @Schema(description = "是否通过：1通过 0拒绝")
    private Integer pass;

    @Schema(description = "审核意见")
    private String reason;

    @Schema(description = "提交时间")
    private LocalDateTime createTime;

    @Schema(description = "审核时间")
    private LocalDateTime reviewTime;
}
