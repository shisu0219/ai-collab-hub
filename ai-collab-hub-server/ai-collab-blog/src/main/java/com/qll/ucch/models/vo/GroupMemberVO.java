package com.qll.ucch.models.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 小组成员行。
 *
 * nickName 这类展示字段本模块查不到（用户表在 sys 模块），
 * 由 admin 层的 Controller 批量回填 —— notice 模块的昵称也是这么处理的。
 *
 * @author 人工智能学院双创平台
 */
@Data
@Schema(description = "小组成员")
public class GroupMemberVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "成员记录ID")
    private Long id;

    @Schema(description = "小组ID")
    private Long groupId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户昵称（由上层回填）")
    private String nickName;

    @Schema(description = "账号（由上层回填）")
    private String account;

    @Schema(description = "成员角色：1组长 2组员 3指导老师")
    private Integer memberRole;

    @Schema(description = "成员角色名称")
    private String memberRoleName;

    @Schema(description = "进组方式：1邀请后同意 2管理员直接拉")
    private Integer joinType;

    @Schema(description = "进组方式名称")
    private String joinTypeName;

    @Schema(description = "状态：0待同意 1已加入 2已拒绝 3已退出")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "加入时间")
    private LocalDateTime joinTime;
}
