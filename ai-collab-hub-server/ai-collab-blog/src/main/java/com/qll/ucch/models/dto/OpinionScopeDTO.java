package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 答辩意见可见性设置入参。
 *
 * 两种用法：
 *   只传 groupId        -> 改小组的全局默认值
 *   同时传 opinionId    -> 改某一条意见的覆盖值
 *
 * scope 取值：0仅组内 1可见摘要 2可见全部
 * 传 null 表示「恢复跟随全局默认」（仅对单条有效）。
 *
 * @author 人工智能学院双创平台
 */
@Data
@Schema(description = "答辩意见可见性设置入参")
public class OpinionScopeDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "小组ID")
    @NotNull(message = "小组ID不能为空")
    private Long groupId;

    @Schema(description = "意见ID；传了就是改单条，不传就是改全局默认")
    private Long opinionId;

    @Schema(description = "可见性：0仅组内 1可见摘要 2可见全部；null 表示恢复跟随全局")
    private Integer scope;
}
