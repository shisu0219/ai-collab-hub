package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 上传头像入参。
 * 这里不接收文件流，文件上传由 integration 模块处理完后把访问地址传进来。
 */
@Data
@Schema(description = "上传头像参数")
public class UpdateAvatarDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "头像访问地址", example = "/upload/avatar/2026/xxx.png")
    @NotBlank(message = "头像地址不能为空")
    private String avatar;
}
