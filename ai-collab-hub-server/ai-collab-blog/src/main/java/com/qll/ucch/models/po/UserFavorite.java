package com.qll.ucch.models.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内容收藏/关注实体，对应新增表 user_favorite。
 * target_type=1 表示收藏文章，target_type=2 表示关注某个用户。
 * 表上有 (user_id, target_type, target_id) 唯一索引，取消收藏走物理删除。
 *
 * @author qll
 */
@Data
@TableName("user_favorite")
public class UserFavorite implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID（谁收藏的） */
    private Long userId;

    /** 收藏对象类型：1文章 2用户 */
    private Integer targetType;

    /** 收藏对象ID */
    private Long targetId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
