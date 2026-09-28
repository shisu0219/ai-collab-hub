package com.qll.ucch.models.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 小组详情返回体。
 *
 * members 里包含组长、组员、指导老师三类；pendingMembers 是「已邀请但还没同意」的，
 * 单独放一个列表，前端才能显示「等待对方同意」的状态。
 *
 * articleId 为 null 表示这个组还没填项目信息（先建组、后填项目的正常状态）。
 *
 * @author 人工智能学院双创平台
 */
@Data
@Schema(description = "小组详情")
public class GroupVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "小组ID")
    private Long id;

    @Schema(description = "小组名称")
    private String name;

    @Schema(description = "小组简介")
    private String intro;

    @Schema(description = "关联项目ID")
    private Long articleId;

    @Schema(description = "关联项目标题")
    private String articleTitle;

    @Schema(description = "组长用户ID")
    private Long leaderId;

    @Schema(description = "组长昵称")
    private String leaderName;

    @Schema(description = "答辩意见组外可见性：0仅组内 1可见摘要 2可见全部")
    private Integer opinionScope;

    @Schema(description = "可见性中文说明，前端直接显示")
    private String opinionScopeName;

    @Schema(description = "答辩意见条数")
    private Integer opinionCount;

    @Schema(description = "已加入的成员列表")
    private List<GroupMemberVO> members;

    @Schema(description = "已邀请待同意的成员列表")
    private List<GroupMemberVO> pendingMembers;

    @Schema(description = "当前登录人是不是这个组的组长（前端据此决定按钮显不显示）")
    private Boolean isLeader;

    @Schema(description = "当前登录人是不是组内人员（组长/组员/指导老师都算）")
    private Boolean isMember;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
