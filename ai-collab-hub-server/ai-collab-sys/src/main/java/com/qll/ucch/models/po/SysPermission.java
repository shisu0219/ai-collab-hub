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
 * 系统权限实体，对应 sys_permission 表。
 */
@Data
@TableName("sys_permission")
@Schema(description = "系统权限")
public class SysPermission implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "权限ID")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 例如 user:audit、article:delete 这种标识 */
    @Schema(description = "权限标识符")
    private String code;

    @Schema(description = "权限描述")
    private String description;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
