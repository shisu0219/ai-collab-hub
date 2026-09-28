package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 学生端登录入参
 */
@Data
@Schema(description = "学生端登录参数")
public class StudentLoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "账号", example = "zhangsan")
    @NotBlank(message = "账号不能为空")
    private String account;

    @Schema(description = "密码", example = "Abc@1234")
    @NotBlank(message = "密码不能为空")
    private String password;
}
