package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理员用户列表的分页查询条件。
 * 所有条件都是可选的，传了就作为过滤条件拼进去。
 */
@Data
@Schema(description = "用户分页查询条件")
public class UserPageQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "账号，模糊匹配")
    private String account;

    @Schema(description = "昵称，模糊匹配")
    private String nickname;

    @Schema(description = "审核状态：0待审 1通过 2拒绝")
    private Integer auditStatus;

    @Schema(description = "是否启用：1启用 0禁用")
    private Integer enable;

    @Schema(description = "角色标识：STUDENT/TEACHER/ADMIN")
    private String roleCode;

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "页码，从 1 开始", example = "1")
    @Min(value = 1, message = "页码不能小于1")
    private Integer current = 1;

    @Schema(description = "每页条数", example = "10")
    @Min(value = 1, message = "每页条数不能小于1")
    private Integer size = 10;
}
