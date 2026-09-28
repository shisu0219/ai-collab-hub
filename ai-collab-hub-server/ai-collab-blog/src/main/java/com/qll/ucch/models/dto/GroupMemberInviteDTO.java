package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 邀请成员入参。
 *
 * 支持一次邀请多个人，组长不用一个个点。
 * memberRole：2组员 3指导老师 —— 组长不能通过邀请产生，
 * 要换组长得走单独的转交接口，避免出现两个组长。
 *
 * @author 人工智能学院双创平台
 */
@Data
@Schema(description = "邀请成员入参")
public class GroupMemberInviteDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "小组ID")
    @NotNull(message = "小组ID不能为空")
    private Long groupId;

    @Schema(description = "被邀请人用户ID列表")
    @NotNull(message = "请选择要邀请的人")
    private List<Long> userIds;

    @Schema(description = "邀请进来的角色：2组员 3指导老师")
    private Integer memberRole = 2;
}
