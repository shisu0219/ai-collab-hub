package com.qll.ucch.models.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户审核记录出参。
 * 把被审用户的基本资料、申请角色一起拼好，管理员在一个接口里就能看全。
 */
@Data
@Schema(description = "用户审核记录")
public class UserReviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "审核记录ID")
    private Long id;

    @Schema(description = "被审核用户ID")
    private Long userId;

    @Schema(description = "被审核用户账号")
    private String account;

    @Schema(description = "被审核用户昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "申请的角色ID")
    private Long roleId;

    @Schema(description = "申请的角色标识")
    private String roleCode;

    @Schema(description = "申请的角色名称")
    private String roleName;

    @Schema(description = "审核结果：0待审 1通过 2拒绝")
    private Integer auditStatus;

    @Schema(description = "证明材料附件地址，多个用英文逗号分隔")
    private String credentials;

    @Schema(description = "证明材料地址列表，前端展示用")
    private List<String> credentialList;

    @Schema(description = "审核意见 / 拒绝原因")
    private String reason;

    @Schema(description = "审核人ID")
    private Long reviewerId;

    @Schema(description = "审核人昵称")
    private String reviewerName;

    @Schema(description = "审核时间")
    private LocalDateTime reviewTime;

    @Schema(description = "申请提交时间")
    private LocalDateTime createTime;
}
