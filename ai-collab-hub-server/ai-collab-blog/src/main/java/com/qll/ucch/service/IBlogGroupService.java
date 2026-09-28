package com.qll.ucch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qll.ucch.models.dto.GroupMemberInviteDTO;
import com.qll.ucch.models.dto.GroupSaveDTO;
import com.qll.ucch.models.po.BlogGroup;
import com.qll.ucch.models.po.BlogGroupMember;
import com.qll.ucch.models.vo.GroupMemberVO;
import com.qll.ucch.models.vo.GroupVO;

import java.util.List;

/**
 * 项目小组服务。
 *
 * 一个小组只能有一个项目；组长可自己建组，管理员也能代建。
 * 进组两条路：邀请后对方同意、管理员直接拉。
 *
 * @author 人工智能学院双创平台
 */
public interface IBlogGroupService extends IService<BlogGroup> {

    /**
     * 建组（组长自己建）。
     *
     * @param leaderId 组长用户ID
     * @param dto      小组信息
     * @return 新建的小组ID
     */
    Long createGroup(Long leaderId, GroupSaveDTO dto);

    /**
     * 管理员代建组。
     * 与自建的区别：可以指定组长、且一并拉进来的成员不需要同意。
     */
    Long createGroupByAdmin(Long adminId, GroupSaveDTO dto);

    /**
     * 改组信息（只有组长和管理员能改）。
     */
    void updateGroup(Long userId, GroupSaveDTO dto);

    /**
     * 查小组详情。组外人员也能看，但看不到成员联系方式等敏感信息。
     */
    GroupVO getGroupDetail(Long groupId, Long viewerId);

    /**
     * 我的小组列表（我当组长的 + 我参与的）。
     */
    List<GroupVO> listMyGroups(Long userId);

    /**
     * 查某个用户作为组长的小组（用于个人主页）。
     */
    GroupVO getGroupByLeader(Long leaderId);

    /**
     * 按项目ID查小组。
     */
    GroupVO getGroupByArticle(Long articleId);

    /**
     * 邀请成员进组（需要对方同意）。
     */
    void inviteMembers(Long operatorId, GroupMemberInviteDTO dto);

    /**
     * 管理员直接把成员拉进组（不需要同意）。
     */
    void addMembersByAdmin(Long adminId, Long groupId, List<Long> userIds, Integer memberRole);

    /**
     * 处理进组邀请（同意/拒绝）。
     */
    void handleInvite(Long userId, Long memberId, Integer accept);

    /**
     * 我的待处理邀请列表。
     */
    List<GroupMemberVO> listMyPendingInvites(Long userId);

    /**
     * 移除成员（组长和管理员可操作）。
     */
    void removeMember(Long operatorId, Long groupId, Long userId);

    /**
     * 转交组长。
     */
    void transferLeader(Long operatorId, Long groupId, Long newLeaderId);

    /**
     * 给小组绑定项目（一个组只能有一个）。
     */
    void bindArticle(Long groupId, Long articleId);

    /**
     * 判断某用户是不是组内人员（组长/组员/指导老师，且状态为已加入）。
     * 答辩意见的可见性判断就靠它 —— 组内看全部。管理员另有判断。
     */
    boolean isGroupMember(Long groupId, Long userId);

    /**
     * 判断某用户是不是该小组的组长。
     */
    boolean isGroupLeader(Long groupId, Long userId);

    /**
     * 取某用户参与的全部小组ID（含自己当组长的）。
     */
    List<Long> listMyGroupIds(Long userId);

    /**
     * 解散小组（只有组长和管理员）。
     */
    void dismissGroup(Long operatorId, Long groupId);
}
