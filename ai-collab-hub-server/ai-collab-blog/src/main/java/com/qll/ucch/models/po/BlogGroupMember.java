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
 * 小组成员实体，对应新增表 blog_group_member。
 *
 * member_role：
 *   1 组长
 *   2 组员（学生）
 *   3 指导老师 —— 指导老师也算「组内人员」，能看全部答辩意见
 *
 * status：
 *   0 待同意（邀请制必须有的态）
 *   1 已加入
 *   2 已拒绝
 *   3 已退出
 *
 * join_type：1 邀请后同意  2 管理员直接拉。留这个字段是为了留痕，
 * 以后要查「这个人是谁弄进来的」能查到。
 *
 * @author 人工智能学院双创平台
 */
@Data
@TableName("blog_group_member")
public class BlogGroupMember implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 小组ID */
    private Long groupId;

    /** 成员用户ID */
    private Long userId;

    /** 成员角色：1组长 2组员 3指导老师 */
    private Integer memberRole;

    /** 进组方式：1邀请后同意 2管理员直接拉 */
    private Integer joinType;

    /** 状态：0待同意 1已加入 2已拒绝 3已退出 */
    private Integer status;

    /** 加入时间 */
    private LocalDateTime joinTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 删除时间 */
    private LocalDateTime deleteTime;
}
