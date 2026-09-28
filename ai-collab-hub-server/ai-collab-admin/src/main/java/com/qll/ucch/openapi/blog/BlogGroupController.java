package com.qll.ucch.openapi.blog;

import com.qll.ucch.exception.BusinessException;
import com.qll.ucch.models.Result;
import com.qll.ucch.models.dto.GroupMemberInviteDTO;
import com.qll.ucch.models.dto.GroupMemberHandleDTO;
import com.qll.ucch.models.dto.GroupSaveDTO;
import com.qll.ucch.models.vo.GroupMemberVO;
import com.qll.ucch.models.vo.GroupVO;
import com.qll.ucch.security.SecurityContext;
import com.qll.ucch.service.IBlogGroupService;
import com.qll.ucch.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 项目小组接口。
 *
 * 【为什么昵称在这里回填】
 * blog 模块依赖不到 sys 模块（依赖链是单向的 common→integration→notice→blog→admin），
 * 所以 Service 里查不到用户昵称。admin 这一层两个模块都能注入，
 * 是天然的聚合层 —— 会话列表的对方昵称也是这么处理的，别往 blog 里硬塞依赖。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@RestController
@RequestMapping("/blog/group")
@RequiredArgsConstructor
@Tag(name = "项目小组")
public class BlogGroupController {

    private final IBlogGroupService blogGroupService;
    private final SysUserService sysUserService;

    // ==================== 建组 / 改组 ====================

    @PostMapping
    @Operation(summary = "创建小组（组长自己建）")
    public Result<Long> create(@Valid @RequestBody GroupSaveDTO dto) {
        Long userId = SecurityContext.requireUserId();
        // 管理员走同一个人口也没问题：如果他传了 leaderId 就按代建处理
        if (SecurityContext.isAdmin() && dto.getLeaderId() != null) {
            return Result.success("创建成功", blogGroupService.createGroupByAdmin(userId, dto));
        }
        return Result.success("创建成功", blogGroupService.createGroup(userId, dto));
    }

    @PutMapping
    @Operation(summary = "修改小组信息 / 答辩意见可见性")
    public Result<Void> update(@Valid @RequestBody GroupSaveDTO dto) {
        Long userId = SecurityContext.requireUserId();
        blogGroupService.updateGroup(userId, dto);
        return Result.success("保存成功", null);
    }

    @DeleteMapping("/{groupId}")
    @Operation(summary = "解散小组")
    public Result<Void> dismiss(@PathVariable("groupId") Long groupId) {
        Long userId = SecurityContext.requireUserId();
        blogGroupService.dismissGroup(userId, groupId);
        return Result.success("已解散", null);
    }

    // ==================== 查询 ====================

    @GetMapping("/my")
    @Operation(summary = "我的小组列表")
    public Result<List<GroupVO>> myGroups() {
        Long userId = SecurityContext.requireUserId();
        List<GroupVO> list = blogGroupService.listMyGroups(userId);
        fillGroupNames(list);
        return Result.success(list);
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "小组详情")
    public Result<GroupVO> detail(@PathVariable("groupId") Long groupId) {
        Long userId = SecurityContext.getUserId();
        GroupVO vo = blogGroupService.getGroupDetail(groupId, userId);
        fillMembers(Collections.singletonList(vo));
        return Result.success(vo);
    }

    @GetMapping("/by-article/{articleId}")
    @Operation(summary = "按项目ID查小组")
    public Result<GroupVO> byArticle(@PathVariable("articleId") Long articleId) {
        GroupVO vo = blogGroupService.getGroupByArticle(articleId);
        if (vo != null) {
            fillMembers(Collections.singletonList(vo));
        }
        return Result.success(vo);
    }

    @GetMapping("/by-leader/{leaderId}")
    @Operation(summary = "查某人担任组长的小组")
    public Result<GroupVO> byLeader(@PathVariable("leaderId") Long leaderId) {
        GroupVO vo = blogGroupService.getGroupByLeader(leaderId);
        if (vo != null) {
            fillMembers(Collections.singletonList(vo));
        }
        return Result.success(vo);
    }

    // ==================== 成员管理 ====================

    @PostMapping("/invite")
    @Operation(summary = "邀请成员进组（需对方同意）")
    public Result<Void> invite(@Valid @RequestBody GroupMemberInviteDTO dto) {
        Long userId = SecurityContext.requireUserId();
        blogGroupService.inviteMembers(userId, dto);
        return Result.success("邀请已发出，等待对方同意", null);
    }

    @PostMapping("/invite/handle")
    @Operation(summary = "同意 / 拒绝进组邀请")
    public Result<Void> handleInvite(@Valid @RequestBody GroupMemberHandleDTO dto) {
        Long userId = SecurityContext.requireUserId();
        blogGroupService.handleInvite(userId, dto.getMemberId(), dto.getAccept());
        return Result.success("已处理", null);
    }

    @GetMapping("/invite/my")
    @Operation(summary = "我的待处理进组邀请")
    public Result<List<GroupMemberVO>> myInvites() {
        Long userId = SecurityContext.requireUserId();
        List<GroupMemberVO> list = blogGroupService.listMyPendingInvites(userId);
        fillMemberNames(list);
        return Result.success(list);
    }

    @DeleteMapping("/member")
    @Operation(summary = "移除成员（组长）")
    public Result<Void> removeMember(@RequestParam("groupId") Long groupId,
                                     @RequestParam("userId") Long userId) {
        Long operator = SecurityContext.requireUserId();
        blogGroupService.removeMember(operator, groupId, userId);
        return Result.success("已移出小组", null);
    }

    @PostMapping("/bind-article")
    @Operation(summary = "把项目绑定到小组（一个小组只能有一个项目）")
    public Result<Void> bindArticle(@RequestParam("groupId") Long groupId,
                                    @RequestParam("articleId") Long articleId) {
        Long operator = SecurityContext.requireUserId();
        // 只有组长和管理员能绑定
        if (!SecurityContext.isAdmin() && !blogGroupService.isGroupLeader(groupId, operator)) {
            throw new BusinessException("只有组长可以绑定项目");
        }
        blogGroupService.bindArticle(groupId, articleId);
        return Result.success("已绑定", null);
    }

    @PostMapping("/transfer-leader")
    @Operation(summary = "转让组长")
    public Result<Void> transferLeader(@RequestParam("groupId") Long groupId,
                                       @RequestParam("newLeaderId") Long newLeaderId) {
        Long operator = SecurityContext.requireUserId();
        blogGroupService.transferLeader(operator, groupId, newLeaderId);
        return Result.success("已转让组长", null);
    }

    // ==================== 管理员接口 ====================

    @PostMapping("/admin/create")
    @Operation(summary = "管理员代建小组")
    public Result<Long> adminCreate(@Valid @RequestBody GroupSaveDTO dto) {
        if (!SecurityContext.isAdmin()) {
            throw new BusinessException("只有管理员可以代建小组");
        }
        Long adminId = SecurityContext.requireUserId();
        return Result.success("创建成功", blogGroupService.createGroupByAdmin(adminId, dto));
    }

    @PostMapping("/admin/add-members")
    @Operation(summary = "管理员直接拉人进组（无需对方同意）")
    public Result<Void> adminAddMembers(@RequestParam("groupId") Long groupId,
                                        @RequestBody List<Long> userIds,
                                        @RequestParam(value = "memberRole", defaultValue = "2") Integer memberRole) {
        if (!SecurityContext.isAdmin()) {
            throw new BusinessException("只有管理员可以直接拉人进组");
        }
        Long adminId = SecurityContext.requireUserId();
        blogGroupService.addMembersByAdmin(adminId, groupId, userIds, memberRole);
        return Result.success("已加入小组", null);
    }

    @GetMapping("/admin/list")
    @Operation(summary = "管理员查看全部小组")
    public Result<List<GroupVO>> adminList() {
        if (!SecurityContext.isAdmin()) {
            throw new BusinessException("只有管理员可以查看全部小组");
        }
        List<GroupVO> list = new ArrayList<>();
        blogGroupService.list().stream()
                .filter(g -> g.getStatus() != null && g.getStatus() == 1)
                .forEach(g -> list.add(blogGroupService.getGroupDetail(g.getId(), SecurityContext.getUserId())));
        fillMembers(list);
        fillGroupNames(list);
        return Result.success(list);
    }

    // ==================== 昵称回填（跨模块取数都在这层做） ====================

    /**
     * 批量回填小组组长昵称。
     * 一次性查出来再填，不要在循环里单条查库。
     */
    private void fillGroupNames(List<GroupVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> ids = new HashSet<>();
        for (GroupVO vo : list) {
            if (vo.getLeaderId() != null) {
                ids.add(vo.getLeaderId());
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = queryNames(new ArrayList<>(ids));
        for (GroupVO vo : list) {
            if (vo.getLeaderId() != null) {
                vo.setLeaderName(nameMap.get(vo.getLeaderId()));
            }
        }
    }

    /**
     * 批量回填小组成员的昵称和账号。
     */
    private void fillMembers(List<GroupVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> ids = new HashSet<>();
        for (GroupVO vo : list) {
            if (vo.getLeaderId() != null) {
                ids.add(vo.getLeaderId());
            }
            if (vo.getMembers() != null) {
                vo.getMembers().forEach(m -> {
                    if (m.getUserId() != null) {
                        ids.add(m.getUserId());
                    }
                });
            }
            if (vo.getPendingMembers() != null) {
                vo.getPendingMembers().forEach(m -> {
                    if (m.getUserId() != null) {
                        ids.add(m.getUserId());
                    }
                });
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = queryNames(new ArrayList<>(ids));
        for (GroupVO vo : list) {
            if (vo.getLeaderId() != null) {
                vo.setLeaderName(nameMap.get(vo.getLeaderId()));
            }
            if (vo.getMembers() != null) {
                fillMemberNames(vo.getMembers());
            }
            if (vo.getPendingMembers() != null) {
                fillMemberNames(vo.getPendingMembers());
            }
        }
    }

    private void fillMemberNames(List<GroupMemberVO> members) {
        if (members == null || members.isEmpty()) {
            return;
        }
        Set<Long> ids = new HashSet<>();
        for (GroupMemberVO m : members) {
            if (m.getUserId() != null) {
                ids.add(m.getUserId());
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = queryNames(new ArrayList<>(ids));
        for (GroupMemberVO m : members) {
            if (m.getUserId() != null) {
                m.setNickName(nameMap.get(m.getUserId()));
            }
        }
    }

    /**
     * 一次查出用户昵称。查不到就返回空 map，让调用方保留原值。
     */
    private Map<Long, String> queryNames(List<Long> userIds) {
        Map<Long, String> map = new HashMap<>();
        try {
            List<com.qll.ucch.models.po.SysUser> users = sysUserService.listByIds(userIds);
            if (users != null) {
                for (com.qll.ucch.models.po.SysUser u : users) {
                    if (u.getId() != null) {
                        map.put(u.getId(), u.getNickname() != null ? u.getNickname() : u.getAccount());
                    }
                }
            }
        } catch (Exception e) {
            // 回填昵称失败不该影响主流程，页面顶多显示空，别让接口挂掉
            log.warn("回填用户昵称失败：{}", e.getMessage());
        }
        return map;
    }
}
