package com.qll.ucch.models.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 邮箱配置实体，对应 sys_email_config 表。
 * 管理员可以配多个发信账号，按 priority 小的先用。
 */
@Data
@TableName("sys_email_config")
@Schema(description = "邮箱配置")
public class SysEmailConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "配置ID")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
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
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
