package com.qll.ucch.models.dto;

import com.qll.ucch.constant.SysConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 找回密码（重置密码）入参 —— 手机号版。
 *
 * 做法是「账号 + 手机号」双重校验，两个都对上才允许重置密码。
 *
 * 【为什么用手机号而不是邮箱】
 *  1. 手机随身带，不像邮箱那样容易忘
 *  2. phone 列本来就有唯一索引，能唯一定位到人
 *  3. 学生注册时留的大多是手机号，邮箱经常随便填或者填错
 *
 * 注意：手机号在注册时是**必填**的，就是为了保证这个找回功能对每个账号都可用。
 * 如果哪天改成选填了，没填手机号的账号就找回不了密码。
 *
 * @author 人工智能学院双创平台
 */
@Data
@Schema(description = "重置密码参数（手机号校验）")
public class ResetByPhoneDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "账号", example = "zhangsan")
    @NotBlank(message = "账号不能为空")
    private String account;

    @Schema(description = "注册时填写的手机号", example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "新密码", example = "Abc@1234")
    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = SysConstants.PASSWORD_REGEX, message = SysConstants.PASSWORD_TIP)
    private String newPassword;
}
