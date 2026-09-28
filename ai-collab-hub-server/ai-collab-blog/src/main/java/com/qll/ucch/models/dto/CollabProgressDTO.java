package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 对接进度更新入参（新增功能）。
 * 对接双方在「我的对接」里把合作进度往前推：待处理 -> 已通过 -> 洽谈中 -> 已合作 -> 已结束。
 *
 * @author qll
 */
@Data
@Schema(description = "对接进度更新入参")
public class CollabProgressDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "对接申请ID")
    @NotNull(message = "申请ID不能为空")
    private Long id;

    @Schema(description = "对接进度：0待处理 1已通过 2洽谈中 3已合作 4已结束 5已拒绝")
    @NotNull(message = "请选择对接进度")
    private Integer collabProgress;

    @Schema(description = "备注/说明，会追加到回复内容里")
    private String remark;
}
