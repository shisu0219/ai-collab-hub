package com.qll.ucch.models.dto;

import com.qll.ucch.constant.SysConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 老师注册入参。
 * <p>
 * 老师是校内角色，跟学生一样不需要填工商信息，
 * 但要多填院系和职称，方便管理员审核时确认身份。
 *
 * @author 人工智能学院双创平台
 */
@Data
@Schema(description = "老师注册参数")
public class TeacherRegisterDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "账号，至少4位", example = "wanglaoshi")
    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = SysConstants.ACCOUNT_REGEX, message = SysConstants.ACCOUNT_TIP)
    private String account;

    @Schema(description = "密码", example = "Abc@1234")
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = SysConstants.PASSWORD_REGEX, message = SysConstants.PASSWORD_TIP)
    private String password;

    @Schema(description = "邮箱", example = "wang@example.edu.cn")
    // 【本次修正】邮箱改为选填：去掉 @NotBlank。
    // @Email 保留 —— 它只在值非空时校验格式，空值直接放行，正好是「选填但填了要合法」。
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128")
    private String email;

    @Schema(description = "手机号（必填）", example = "13800138000")
    // 必填：找回密码要用「账号 + 手机号」双重校验，不填就没法找回
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "姓名", example = "王老师")
    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名长度不能超过64")
    private String nickname;

    @Schema(description = "所属院系", example = "人工智能学院")
    @NotBlank(message = "所属院系不能为空")
    @Size(max = 128, message = "院系名称过长")
    private String department;

    @Schema(description = "职称", example = "副教授")
    @Size(max = 32, message = "职称过长")
    private String title;

    @Schema(description = "证明材料附件地址，多个用英文逗号分隔（工牌/教师资格证等）")
    @Size(max = 2000, message = "证明材料过长")
    private String credentials;
}
