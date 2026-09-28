package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 操作日志查询入参。
 *
 * @author 人工智能学院双创平台
 */
@Data
@Schema(description = "操作日志查询条件")
public class OperationLogQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码，从 1 开始")
    @Min(value = 1, message = "页码不能小于 1")
    private Long pageNumber = 1L;

    @Schema(description = "每页条数")
    @Min(value = 1, message = "每页条数不能小于 1")
    private Long pageSize = 10L;

    /** 关键词，同时匹配操作人账号和操作动作 */
    @Schema(description = "关键词（操作人账号 / 操作动作）")
    private String keyword;

    /** 业务模块 */
    @Schema(description = "业务模块")
    private String module;

    /** 操作人用户ID */
    @Schema(description = "操作人用户ID")
    private Long userId;

    /** 开始日期，格式 yyyy-MM-dd */
    @Schema(description = "开始日期")
    private String startDate;

    /** 结束日期，格式 yyyy-MM-dd */
    @Schema(description = "结束日期")
    private String endDate;
}
