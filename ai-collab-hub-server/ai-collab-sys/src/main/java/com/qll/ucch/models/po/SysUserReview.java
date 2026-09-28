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
 * 用户审核记录实体，对应 sys_user_review 表。
 * 学生/老师注册后生成一条待审记录，管理员审核时回填 reviewer_id 和 review_time。
 */
@Data
@TableName("sys_user_review")
@Schema(description = "用户审核记录")
public class SysUserReview implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "审核记录ID")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description = "被审核的用户ID")
    private Long userId;

    @Schema(description = "申请的角色ID")
    private Long roleId;

    /** 0 待审，1 通过，2 拒绝 */
    @Schema(description = "审核结果：0待审 1通过 2拒绝")
    private Integer auditStatus;

    /** 学生可能是学信网截图、学生证；老师是工牌、教师资格证等，多个用英文逗号分隔 */
    @Schema(description = "证明材料附件地址，逗号分隔")
    private String credentials;

    @Schema(description = "审核意见 / 拒绝原因")
    private String reason;

    @Schema(description = "审核人ID")
    private Long reviewerId;

    @Schema(description = "审核时间")
    private LocalDateTime reviewTime;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
