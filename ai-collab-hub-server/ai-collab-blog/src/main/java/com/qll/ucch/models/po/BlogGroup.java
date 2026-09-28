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
 * 项目小组实体，对应新增表 blog_group。
 *
 * 一个小组只能有一个项目，这条约束在库里用 uk_group_article 唯一索引卡死，
 * 代码里再判一次只是为了给出好看的提示语，不能只靠代码。
 *
 * opinion_scope 是「答辩意见对组外人员的可见性」的全局默认值，
 * 组员和指导老师不受它影响，永远能看全部。
 *
 * @author 人工智能学院双创平台
 */
@Data
@TableName("blog_group")
public class BlogGroup implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 小组ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 小组名称 */
    private String name;

    /** 关联的项目ID（一个小组只能有一个项目） */
    private Long articleId;

    /** 组长用户ID */
    private Long leaderId;

    /** 小组简介 */
    private String intro;

    /** 答辩意见组外可见性（全局默认）：0仅组内 1可见摘要 2可见全部 */
    private Integer opinionScope;

    /** 小组状态：1正常 0已解散 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人ID */
    private Long createBy;

    /** 更新人ID */
    private Long updateBy;

    /** 删除时间 */
    private LocalDateTime deleteTime;
}
