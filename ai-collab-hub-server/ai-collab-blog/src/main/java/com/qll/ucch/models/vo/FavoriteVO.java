package com.qll.ucch.models.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 我的收藏列表项。
 * 收藏文章时带上文章的简要信息；关注用户时 targetId 就是用户ID，昵称由上层填充。
 *
 * @author qll
 */
@Data
@Schema(description = "收藏项")
public class FavoriteVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "收藏记录ID")
    private Long id;

    @Schema(description = "收藏对象类型：1文章 2用户")
    private Integer targetType;

    @Schema(description = "收藏对象类型名称")
    private String targetTypeName;

    @Schema(description = "收藏对象ID")
    private Long targetId;

    @Schema(description = "收藏时间")
    private LocalDateTime createTime;

    // ---------------- targetType=1 时有值 ----------------

    @Schema(description = "文章标题")
    private String articleTitle;

    @Schema(description = "文章类型ID")
    private Long typeId;

    @Schema(description = "文章类型名称")
    private String typeName;

    @Schema(description = "文章所属地区")
    private String location;

    @Schema(description = "文章状态ID")
    private Long statusId;

    @Schema(description = "文章状态名称")
    private String statusName;

    @Schema(description = "文章是否已被删除/下架")
    private Boolean articleAvailable;

    // ---------------- targetType=2 时有值 ----------------

    @Schema(description = "被关注用户昵称（占位，上层填充）")
    private String targetName;
}
