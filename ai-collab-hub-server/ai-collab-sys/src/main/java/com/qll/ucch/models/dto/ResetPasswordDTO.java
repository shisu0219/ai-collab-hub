package com.qll.ucch.models.dto;

import com.qll.ucch.constant.SysConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 找回密码（重置密码）入参 —— 邮箱版。
 *
 * 和 {@link ResetByPhoneDTO} 是**并行的两种找回方式**，任选其一都能重置密码：
 * <pre>
 *   邮箱版：账号 + 邮箱     POST /user/find/pwd/email
 *   手机号版：账号 + 手机号   POST /user/find/pwd/phone
 * </pre>
 *
 * 【两种都留着的理由】
 *  1. 用户可能只记得其中一个 —— 邮箱是注册时要的，手机是随身带的
 *  2. 都属于合法功能，各自独立，删一个对另一个没好处
 *  3. 前端现在默认走手机号版，但邮箱版留作备用入口
 *
 * ⚠️ 注意：邮箱在注册时是**选填**的，所以没填邮箱的账号用不了邮箱找回 ——
 * 这类账号走手机号版（手机号是注册必填的，人人都有）。
 *
 * @author qll
 */
@Data
@Schema(description = "重置密码参数（邮箱校验）")
public class ResetPasswordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "账号", example = "zhangsan")
    @NotBlank(message = "账号不能为空")
    private String account;

    @Schema(description = "注册时填写的邮箱", example = "zhangsan@example.edu.cn")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "新密码", example = "Abc@1234")
    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = SysConstants.PASSWORD_REGEX, message = SysConstants.PASSWORD_TIP)
    private String newPassword;
}
