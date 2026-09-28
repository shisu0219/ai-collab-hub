package com.qll.ucch.models.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录成功后返回给前端的信息。
 * 前端拿 roleCodes 判断进哪个端的工作台，拿 auditStatus 决定要不要显示「审核中」提示页。
 */
@Data
@Schema(description = "登录结果")
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private Long userId;

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

    @Schema(description = "角色标识列表，例如 [STUDENT]")
    private List<String> roleCodes;

    @Schema(description = "角色名称列表")
    private List<String> roleNames;

    /** 令牌信息占位，由 Controller / security 模块填充 */
    @Schema(description = "令牌信息")
    private TokenInfoVO tokenInfo;
}
