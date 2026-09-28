package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 开临时沟通房间入参。
 *
 * 一般由「申请通过」时自动创建，也可以手动补开（比如房间过期了想续）。
 * hours 不传时用默认时长。
 *
 * @author 人工智能学院双创平台
 */
@Data
@Schema(description = "开临时沟通房间入参")
public class TmpRoomCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关联的对接申请ID，二选一")
    private Long registrationId;

    @Schema(description = "关联的项目ID，二选一")
    private Long articleId;

    @Schema(description = "有效时长（小时），不传用默认值")
    private Integer hours;

    @Schema(description = "参与人A（申请方）。一般由后端从申请记录里带出来，前端不用传")
    private Long userA;

    @Schema(description = "参与人B（项目组长/发布方）。同上，后端带出来")
    private Long userB;

    @Schema(description = "项目标题，用作房间名。后端带出来")
    private String title;
}
