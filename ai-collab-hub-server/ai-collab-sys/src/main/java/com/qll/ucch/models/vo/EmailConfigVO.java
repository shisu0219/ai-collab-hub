package com.qll.ucch.models.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 邮箱配置出参。
 * 只有管理员能看，所以密码没做脱敏，方便管理员复制出来核对。
 */
@Data
@Schema(description = "邮箱配置")
public class EmailConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "配置ID")
    private Long id;

    @Schema(description = "SMTP 服务器地址")
    private String smtpHost;

    @Schema(description = "SMTP 端口")
    private Integer smtpPort;

    @Schema(description = "发信账号")
    private String smtpUsername;

    @Schema(description = "发信密码 / 授权码")
    private String smtpPassword;

    @Schema(description = "优先级，数字越小越优先")
    private Integer priority;

    @Schema(description = "是否启用 SSL：1是 0否")
    private Integer sslEnable;

    @Schema(description = "连接超时（毫秒）")
    private Integer connectionTimeout;

    @Schema(description = "读取超时（毫秒）")
    private Integer readTimeout;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
