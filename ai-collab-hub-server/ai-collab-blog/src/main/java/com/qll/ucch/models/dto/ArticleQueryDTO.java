package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文章查询入参（列表 / 我的发布 / 内容审核共用）。
 * 新增的搜索能力就落在这里：title 模糊、location、tagId、typeId、statusId 组合过滤。
 *
 * @author qll
 */
@Data
@Schema(description = "文章查询入参")
public class ArticleQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "标题，模糊匹配")
    private String title;

    @Schema(description = "地区/地点")
    private String location;

    @Schema(description = "标签ID")
    private Long tagId;

    @Schema(description = "类型ID：1项目 2需求 3课程")
    private Long typeId;

    @Schema(description = "状态ID：1待审核 2已发布 3已拒绝 4已下架")
    private Long statusId;

    @Schema(description = "进度ID")
    private Long progressId;

    @Schema(description = "发布者用户ID，按人筛（个人主页聚合用）")
    private Long userId;

    @Schema(description = "页码，从 1 开始")
    private Long pageNum = 1L;

    @Schema(description = "每页条数")
    private Long pageSize = 10L;
}
