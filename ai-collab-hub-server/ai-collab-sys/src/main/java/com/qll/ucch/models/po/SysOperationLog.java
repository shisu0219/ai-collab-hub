package com.qll.ucch.models.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志（新增功能）。
 * <p>
 * 原来的平台谁改了什么完全没留痕，出问题查不到。这里把管理员的
 * 审核、启用禁用、改内容这些动作都记一笔。
 * <p>
 * 注意：这张表只增不改不删，所以没加 delete_time，也没加一堆审计字段，
 * 保持轻量——日志表最怕字段多、写起来慢。
 *
 * @author 人工智能学院双创平台
 */
@Data
@TableName("sys_operation_log")
@Schema(description = "操作日志")
public class SysOperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日志ID */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "日志ID")
    private Long id;

    /** 操作人用户ID */
    @Schema(description = "操作人用户ID")
    private Long userId;

    /** 操作人账号，冗余存一份，查日志时不用再联表 */
    @Schema(description = "操作人账号")
    private String userAccount;

    /** 模块：用户审核 / 内容审核 / 发布内容 / 对接申请 / 账户管理 */
    @Schema(description = "业务模块")
    private String module;

    /** 操作动作，一句话说明干了什么 */
    @Schema(description = "操作动作")
    private String action;

    /** 操作对象ID */
    @Schema(description = "操作对象ID")
    private Long targetId;

    /** 操作详情 */
    @Schema(description = "操作详情")
    private String detail;

    /** 操作IP */
    @Schema(description = "操作IP")
    private String ip;

    /** 操作时间 */
    @Schema(description = "操作时间")
    private LocalDateTime createTime;
}
