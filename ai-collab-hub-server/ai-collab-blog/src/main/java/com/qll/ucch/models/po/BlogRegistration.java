package com.qll.ucch.models.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对接申请实体，对应 blog_registration。
 * 学生申请老师的需求、老师对学生的项目感兴趣，两种场景都往这张表写。
 * collab_progress 是新增字段，用来做对接进度流转。
 *
 * @author qll
 */
@Data
@TableName("blog_registration")
public class BlogRegistration implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 申请ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 目标文章ID */
    private Long articleId;

    /** 文章类型ID */
    private Long typeId;

    /** 申请人用户ID */
    private Long userId;

    /** 年级，如 25级 */
    private String grade;

    /** 班级，如 大数据4班 */
    private String className;

    /** 擅长部分（预设技能，逗号分隔） */
    private String skills;

    /** 补充说明（选填，原"申请原因"） */
    private String reason;

    /** 联系方式类型（微信/QQ/电话/邮箱） */
    private String contactWay;

    /** 联系方式值 */
    private String contactWayValue;

    /** 附件地址列表 */
    private String attachments;

    /** 处理结果：1通过 0拒绝 null待处理 */
    private Integer pass;

    /** 回复内容 */
    private String reviewMessage;

    /** 对接进度状态：0待处理 1已通过 2洽谈中 3已合作 4已结束 5已拒绝 */
    private Integer collabProgress;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 删除时间，非空表示已逻辑删除 */
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deleteTime;

    /** 创建人ID */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /** 更新人ID */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}
