package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改当前登录用户信息的入参。
 * 账号不让改（改了别人找不到你），密码走单独的接口，所以这里都没有。
 * 字段留空表示不修改该字段。
 */
@Data
@Schema(description = "修改用户信息参数")
public class UpdateUserInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "昵称", example = "张三")
    @Size(max = 64, message = "昵称长度不能超过64")
    private String nickname;

    @Schema(description = "邮箱", example = "zhangsan@example.edu.cn")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "头像访问地址")
    private String avatar;
}
