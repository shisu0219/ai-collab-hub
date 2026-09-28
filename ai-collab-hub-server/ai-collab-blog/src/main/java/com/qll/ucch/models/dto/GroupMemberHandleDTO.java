package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 处理进组邀请入参。
 *
 * 用 Integer 而不是 Boolean 收 accept，是因为 Boolean 传 null 时
 * 反序列化会静默变成 false，等于「没表态就当拒绝」。用 1/0 更明确。
 *
 * @author 人工智能学院双创平台
 */
@Data
@Schema(description = "处理进组邀请入参")
public class GroupMemberHandleDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "成员记录ID")
    @NotNull(message = "成员记录ID不能为空")
    private Long memberId;

    @Schema(description = "是否同意：1同意 0拒绝")
    @NotNull(message = "请选择同意或拒绝")
    private Integer accept;
}
