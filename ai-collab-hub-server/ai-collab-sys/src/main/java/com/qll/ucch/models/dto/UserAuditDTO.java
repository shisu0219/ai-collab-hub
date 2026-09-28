package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户审核（通过 / 拒绝）的入参。
 * reviewId 可以不传，不传就按 userId 找这个人最新一条待审记录。
 */
@Data
@Schema(description = "用户审核参数")
public class UserAuditDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "被审核的用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "审核记录ID，不传则自动取该用户最新一条待审记录")
    private Long reviewId;

    @Schema(description = "审核意见 / 拒绝原因，拒绝时必填")
    @Size(max = 500, message = "审核意见长度不能超过500")
    private String reason;
}
