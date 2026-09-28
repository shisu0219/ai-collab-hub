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
 * 答辩意见实体，对应新增表 blog_opinion。
 *
 * 这里的「意见」不是论坛评论，而是答辩现场老师方/专业方给出的答辩问题和意见，
 * 由管理员整理后上传。基本形态是「一份文档/表格 + 逐条整理出来的条目」。
 *
 * source 是自由文本（如「张老师」「校外专家」），不关联 sys_user ——
 * 按需求「目前不需要精确到人」。
 *
 * scope_override 是本条的组外可见性覆盖：
 *   NULL = 跟随 blog_group.opinion_scope（全局默认）
 *   0/1/2 = 覆盖本条的设置
 *
 * @author 人工智能学院双创平台
 */
@Data
@TableName("blog_opinion")
public class BlogOpinion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 意见ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联项目ID */
    private Long articleId;

    /** 关联小组ID */
    private Long groupId;

    /** 问题/意见标题 */
    private String title;

    /** 问题与意见正文 */
    private String content;

    /** 类型：1答辩问题 2答辩意见 3修改建议 */
    private Integer opinionType;

    /** 来源（自由文本，如「张老师」「校外专家」） */
    private String source;

    /** 关联资料地址列表（管理员上传的文档/表格） */
    private String attachments;

    /** 本条组外可见性（覆盖全局）：NULL跟随小组 0仅组内 1可见摘要 2可见全部 */
    private Integer scopeOverride;

    /** 上传的管理员用户ID */
    private Long uploaderId;

    /** 视频访问地址（下一轮启用） */
    private String videoUrl;

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
