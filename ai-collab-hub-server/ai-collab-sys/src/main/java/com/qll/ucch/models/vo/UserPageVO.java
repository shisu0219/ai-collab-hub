package com.qll.ucch.models.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理员用户列表出参。
 * 列表页不需要个人简介那种大字段，所以单独一个 VO，别拿 UserInfoVO 硬凑。
 */
@Data
@Schema(description = "用户列表项")
public class UserPageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "账号")
    private String account;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "是否启用：1启用 0禁用")
    private Integer enable;

    @Schema(description = "审核状态：0待审 1通过 2拒绝")
    private Integer auditStatus;

    @Schema(description = "角色标识，多个用逗号拼起来，方便列表直接展示")
    private String roleCodes;

    @Schema(description = "角色名称，多个用逗号拼起来")
    private String roleNames;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
