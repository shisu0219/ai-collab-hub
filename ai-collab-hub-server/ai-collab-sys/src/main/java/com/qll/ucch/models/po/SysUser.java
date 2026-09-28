package com.qll.ucch.models.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户实体，对应 sys_user 表。
 * 平台里学生、老师、管理员都存在这张表里，靠 sys_user_role 区分角色。
 */
@Data
@TableName("sys_user")
@Schema(description = "系统用户")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description = "用户账号，唯一")
    private String account;

    @Schema(description = "用户密码，BCrypt 加密后存储")
    private String password;

    @Schema(description = "用户邮箱，唯一")
    private String email;

    @Schema(description = "手机号，可为空")
    private String phone;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "头像访问地址")
    private String avatar;

    /** 1 启用，0 禁用。管理员可以随时禁用账号 */
    @Schema(description = "是否启用：1启用 0禁用")
    private Integer enable;

    /** 0 待审核，1 审核通过，2 审核拒绝。注册后默认 0 */
    @Schema(description = "审核状态：0待审 1通过 2拒绝")
    private Integer auditStatus;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "创建人ID")
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    @Schema(description = "更新人ID")
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /** 逻辑删除字段，MyBatis-Plus 会自动在查询里拼 delete_time IS NULL */
    @Schema(description = "删除时间，为空表示未删除")
    @TableLogic(value = "null", delval = "now()")
    @TableField("delete_time")
    private LocalDateTime deleteTime;
}
