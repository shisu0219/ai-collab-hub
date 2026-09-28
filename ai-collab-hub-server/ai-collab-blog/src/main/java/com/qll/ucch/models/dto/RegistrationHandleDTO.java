package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 对接申请处理入参（通过 / 拒绝）。
 * pass=1 通过时会把进度推到「已通过」，pass=0 拒绝时推到「已拒绝」。
 *
 * @author qll
 */
@Data
@Schema(description = "对接申请处理入参")
public class RegistrationHandleDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "申请ID")
    @NotNull(message = "申请ID不能为空")
    private Long id;

    @Schema(description = "处理结果：1通过 0拒绝")
    @NotNull(message = "请选择处理结果")
    private Integer pass;

    @Schema(description = "回复内容/拒绝理由")
    private String reviewMessage;
}
