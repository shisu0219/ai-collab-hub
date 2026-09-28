package com.qll.ucch.models.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志出参。
 *
 * @author 人工智能学院双创平台
 */
@Data
@Schema(description = "操作日志")
public class OperationLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "操作人用户ID")
    private Long userId;

    @Schema(description = "操作人账号")
    private String userAccount;

    @Schema(description = "操作人昵称")
    private String nickname;

    @Schema(description = "业务模块")
    private String module;

    @Schema(description = "操作动作")
    private String action;

    @Schema(description = "操作对象ID")
    private Long targetId;

    @Schema(description = "操作详情")
    private String detail;

    @Schema(description = "操作IP")
    private String ip;

    @Schema(description = "操作时间")
    private LocalDateTime createTime;
}
