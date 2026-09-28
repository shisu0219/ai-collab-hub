package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 邮箱配置新增 / 修改入参
 */
@Data
@Schema(description = "邮箱配置参数")
public class EmailConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "配置ID，新增时不传，修改时必传")
    private Long id;

    @Schema(description = "SMTP 服务器地址", example = "smtp.qq.com")
    @NotBlank(message = "SMTP 服务器地址不能为空")
    private String smtpHost;

    @Schema(description = "SMTP 端口", example = "465")
    @NotNull(message = "SMTP 端口不能为空")
    @Min(value = 1, message = "端口不合法")
    @Max(value = 65535, message = "端口不合法")
    private Integer smtpPort;

    @Schema(description = "发信账号", example = "noreply@example.com")
    @NotBlank(message = "发信账号不能为空")
    private String smtpUsername;

    @Schema(description = "发信密码 / 授权码")
    @NotBlank(message = "发信密码不能为空")
    private String smtpPassword;

    @Schema(description = "优先级，数字越小越优先", example = "1")
    @NotNull(message = "优先级不能为空")
    @Min(value = 1, message = "优先级最小为1")
    private Integer priority;

    @Schema(description = "是否启用 SSL：1是 0否", example = "1")
    private Integer sslEnable = 1;

    @Schema(description = "连接超时（毫秒）", example = "5000")
    private Integer connectionTimeout = 5000;

    @Schema(description = "读取超时（毫秒）", example = "5000")
    private Integer readTimeout = 5000;
}
