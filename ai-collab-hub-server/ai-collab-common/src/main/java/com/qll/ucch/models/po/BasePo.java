package com.qll.ucch.models.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 所有实体类的公共字段基类。
 * 建表脚本里每张表基本都有这几个字段，抽出来省得重复写。
 *
 * @author 人工智能学院双创平台
 */
@Data
public class BasePo implements Serializable {

    /** 创建时间，插入时自动填充 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，插入和更新时都自动填充 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人ID */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    /** 更新人ID */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /** 删除时间，逻辑删除用。非空表示已删除 */
    @TableField(value = "delete_time")
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deleteTime;
}
