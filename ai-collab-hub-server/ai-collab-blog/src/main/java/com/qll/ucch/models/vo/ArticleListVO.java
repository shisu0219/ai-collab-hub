package com.qll.ucch.models.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章列表项出参（简要信息）。
 * 列表页只需要标题、类型、状态这些，扩展表内容不进这里，详情接口才返回。
 *
 * @author qll
 */
@Data
@Schema(description = "文章列表项")
public class ArticleListVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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

    @Schema(description = "详细描述（列表里截断展示，由前端处理）")
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

    @Schema(description = "发布者昵称（由上层回填）")
    private String publisherName;

    @Schema(description = "发布者角色编码：STUDENT/TEACHER，列表用它显示「老师发布」标识")
    private String publisherRoleCode;

    @Schema(description = "发布者角色名称")
    private String publisherRoleName;

    @Schema(description = "所属小组ID")
    private Long groupId;

    @Schema(description = "所属小组名称")
    private String groupName;

    @Schema(description = "发布时间")
    private LocalDateTime createTime;
}
