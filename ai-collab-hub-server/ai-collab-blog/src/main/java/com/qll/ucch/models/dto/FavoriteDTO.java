package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 收藏/关注操作入参（新增功能）。
 * targetType=1 收藏文章，targetType=2 关注用户。
 *
 * @author qll
 */
@Data
@Schema(description = "收藏/关注入参")
public class FavoriteDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "收藏对象类型：1文章 2用户")
    @NotNull(message = "请选择收藏类型")
    private Integer targetType;

    @Schema(description = "收藏对象ID（文章ID 或 用户ID）")
    @NotNull(message = "收藏对象不能为空")
    private Long targetId;
}
