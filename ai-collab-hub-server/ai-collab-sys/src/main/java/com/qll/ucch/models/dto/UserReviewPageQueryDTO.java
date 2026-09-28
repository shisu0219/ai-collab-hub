package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 待审核用户列表的分页查询条件
 */
@Data
@Schema(description = "用户审核分页查询条件")
public class UserReviewPageQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "审核状态，不传默认查待审核(0)：0待审 1通过 2拒绝")
    private Integer auditStatus;

    @Schema(description = "被审核用户的账号，模糊匹配")
    private String account;

    @Schema(description = "被审核用户的昵称，模糊匹配")
    private String nickname;

    @Schema(description = "申请角色ID，例如只查学生申请就传 2")
    private Long roleId;

    @Schema(description = "申请角色标识：STUDENT/TEACHER")
    private String roleCode;

    @Schema(description = "页码，从 1 开始", example = "1")
    @Min(value = 1, message = "页码不能小于1")
    private Integer current = 1;

    @Schema(description = "每页条数", example = "10")
    @Min(value = 1, message = "每页条数不能小于1")
    private Integer size = 10;
}
