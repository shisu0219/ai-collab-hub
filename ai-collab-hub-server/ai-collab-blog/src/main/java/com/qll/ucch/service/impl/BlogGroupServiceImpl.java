package com.qll.ucch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qll.ucch.exception.BusinessException;
import com.qll.ucch.mapper.BlogGroupMapper;
import com.qll.ucch.mapper.BlogGroupMemberMapper;
import com.qll.ucch.models.dto.GroupMemberInviteDTO;
import com.qll.ucch.models.dto.GroupSaveDTO;
import com.qll.ucch.models.po.BlogGroup;
import com.qll.ucch.models.po.BlogGroupMember;
import com.qll.ucch.models.vo.GroupMemberVO;
import com.qll.ucch.models.vo.GroupVO;
import com.qll.ucch.service.IBlogGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目小组服务实现。
 *
 * 【几条硬规则，改的时候别绕过去】
 * 1. 一个小组只能有一个项目 —— 库里有 uk_group_article 唯一索引兜底，
 *    代码里也判一次，只是为了报错好看。
 * 2. 一个用户在一个组里只有一条成员记录（uk_group_user），
 *    重复邀请要更新那条记录的状态，不能新增，否则唯一索引会报错。
 * 3. 组长不能被「移除」，要换人得走 transferLeader。
 * 4. 指导老师算组内人员，和组员一样能看全部答辩意见。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogGroupServiceImpl extends ServiceImpl<BlogGroupMapper, BlogGroup>
        implements IBlogGroupService {

    /** 成员角色：组长 */
    public static final int ROLE_LEADER = 1;
    /** 成员角色：组员 */
    public static final int ROLE_MEMBER = 2;
    /** 成员角色：指导老师 */
    public static final int ROLE_TEACHER = 3;

    /** 成员状态：待同意 */
    public static final int ST_PENDING = 0;
    /** 成员状态：已加入 */
    public static final int ST_JOINED = 1;
    /** 成员状态：已拒绝 */
    public static final int ST_REJECTED = 2;
    /** 成员状态：已退出 */
    public static final int ST_QUIT = 3;

    /** 进组方式：邀请后同意 */
    public static final int JOIN_BY_INVITE = 1;
    /** 进组方式：管理员直接拉 */
    public static final int JOIN_BY_ADMIN = 2;

    private final BlogGroupMemberMapper groupMemberMapper;

    // ==================== 建组 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGroup(Long leaderId, GroupSaveDTO dto) {
        if (leaderId == null) {
            throw new BusinessException("请先登录");
        }
        // 同一个人已经建过组就不让再建 —— 一个组长对应一个项目
        BlogGroup exist = getOne(new LambdaQueryWrapper<BlogGroup>()
                .eq(BlogGroup::getLeaderId, leaderId)
                .eq(BlogGroup::getStatus, 1)
                .last("limit 1"));
        if (exist != null) {
            throw new BusinessException("你已經是「" + exist.getName() + "」的组长了，一个人只能带一个小组");
        }

        BlogGroup group = new BlogGroup();
        group.setName(dto.getName());
        group.setIntro(dto.getIntro());
        group.setLeaderId(leaderId);
        group.setArticleId(dto.getArticleId());
        // 需求确认：组外默认「可见摘要」
        group.setOpinionScope(dto.getOpinionScope() == null ? 1 : dto.getOpinionScope());
        group.setStatus(1);
        group.setCreateBy(leaderId);
        group.setUpdateBy(leaderId);
        save(group);

        // 组长自己也写一条成员记录，这样「组内成员」判断逻辑统一
        addMemberRow(group.getId(), leaderId, ROLE_LEADER, JOIN_BY_INVITE, ST_JOINED);

        // 建组时一并邀请的人
        List<Long> memberIds = dto.getMemberIds() == null ? Collections.emptyList() : dto.getMemberIds();
        for (Long uid : memberIds) {
            if (uid != null && !uid.equals(leaderId)) {
                addMemberRow(group.getId(), uid, ROLE_MEMBER, JOIN_BY_INVITE, ST_PENDING);
            }
        }
        List<Long> teacherIds = dto.getTeacherIds() == null ? Collections.emptyList() : dto.getTeacherIds();
        for (Long uid : teacherIds) {
            if (uid != null && !uid.equals(leaderId)) {
                addMemberRow(group.getId(), uid, ROLE_TEACHER, JOIN_BY_INVITE, ST_PENDING);
            }
        }

        log.info("用户 {} 创建小组 {}（{}），一并邀请 {} 名成员、{} 名老师",
                leaderId, group.getId(), group.getName(), memberIds.size(), teacherIds.size());
        return group.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGroupByAdmin(Long adminId, GroupSaveDTO dto) {
        if (dto.getLeaderId() == null) {
            throw new BusinessException("管理员代建小组必须指定组长");
        }
        Long groupId = createGroup(dto.getLeaderId(), dto);
        // 管理员拉进来的成员直接生效，不需要同意 —— 这是管理员和市场成员的区别
        if (!CollectionUtils.isEmpty(dto.getMemberIds())) {
            for (Long uid : dto.getMemberIds()) {
                approveIfPending(groupId, uid, JOIN_BY_ADMIN);
            }
        }
        if (!CollectionUtils.isEmpty(dto.getTeacherIds())) {
            for (Long uid : dto.getTeacherIds()) {
                approveIfPending(groupId, uid, JOIN_BY_ADMIN);
            }
        }
        log.info("管理员 {} 代建小组 {}，组长 {}", adminId, groupId, dto.getLeaderId());
        return groupId;
    }

    // ==================== 改 / 解散 ====================

    @Override
    public void updateGroup(Long userId, GroupSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("小组ID不能为空");
        }
        BlogGroup group = getById(dto.getId());
        if (group == null) {
            throw new BusinessException("小组不存在");
        }
        if (StringUtils.hasText(dto.getName())) {
            group.setName(dto.getName());
        }
        group.setIntro(dto.getIntro());
        // 可见性只有组长能改，普通组员改不了 —— 这是需求里明确要的
        if (dto.getOpinionScope() != null) {
            if (!userId.equals(group.getLeaderId())) {
                throw new BusinessException("只有组长可以设置答辩意见的可见范围");
            }
            group.setOpinionScope(dto.getOpinionScope());
        }
        group.setUpdateBy(userId);
        updateById(group);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dismissGroup(Long operatorId, Long groupId) {
        BlogGroup group = getById(groupId);
        if (group == null) {
            throw new BusinessException("小组不存在");
        }
        if (!operatorId.equals(group.getLeaderId())) {
            throw new BusinessException("只有组长可以解散小组");
        }
        group.setStatus(0);
        group.setUpdateBy(operatorId);
        updateById(group);
        log.info("用户 {} 解散小组 {}", operatorId, groupId);
    }

    // ==================== 查询 ====================

    @Override
    public GroupVO getGroupDetail(Long groupId, Long viewerId) {
        BlogGroup group = getById(groupId);
        if (group == null) {
            throw new BusinessException("小组不存在");
        }
        return toVO(group, viewerId, true);
    }

    @Override
    public List<GroupVO> listMyGroups(Long userId) {
        // 先找出我参与的所有组ID（含自己当组长的）
        List<Long> groupIds = listMyGroupIds(userId);
        if (CollectionUtils.isEmpty(groupIds)) {
            return Collections.emptyList();
        }
        List<BlogGroup> groups = listByIds(groupIds);
        return groups.stream()
                .filter(g -> g.getStatus() != null && g.getStatus() == 1)
                .map(g -> toVO(g, userId, false))
                .collect(Collectors.toList());
    }

    @Override
    public GroupVO getGroupByLeader(Long leaderId) {
        BlogGroup group = getOne(new LambdaQueryWrapper<BlogGroup>()
                .eq(BlogGroup::getLeaderId, leaderId)
                .eq(BlogGroup::getStatus, 1)
                .last("limit 1"));
        return group == null ? null : toVO(group, null, false);
    }

    @Override
    public GroupVO getGroupByArticle(Long articleId) {
        if (articleId == null) {
            return null;
        }
        BlogGroup group = getOne(new LambdaQueryWrapper<BlogGroup>()
                .eq(BlogGroup::getArticleId, articleId)
                .eq(BlogGroup::getStatus, 1)
                .last("limit 1"));
        return group == null ? null : toVO(group, null, false);
    }

    // ==================== 成员管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inviteMembers(Long operatorId, GroupMemberInviteDTO dto) {
        BlogGroup group = getById(dto.getGroupId());
        if (group == null) {
            throw new BusinessException("小组不存在");
        }
        if (!operatorId.equals(group.getLeaderId())) {
            throw new BusinessException("只有组长可以邀请成员");
        }
        int role = dto.getMemberRole() == null ? ROLE_MEMBER : dto.getMemberRole();
        if (role == ROLE_LEADER) {
            throw new BusinessException("不能通过邀请产生第二个组长，请使用转让组长");
        }
        if (CollectionUtils.isEmpty(dto.getUserIds())) {
            throw new BusinessException("请选择要邀请的人");
        }
        for (Long uid : dto.getUserIds()) {
            if (uid == null || uid.equals(operatorId)) {
                continue;
            }
            addMemberRow(dto.getGroupId(), uid, role, JOIN_BY_INVITE, ST_PENDING);
        }
        log.info("组长 {} 邀请 {} 人进入小组 {}，角色 {}", operatorId, dto.getUserIds().size(),
                dto.getGroupId(), role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMembersByAdmin(Long adminId, Long groupId, List<Long> userIds, Integer memberRole) {
        BlogGroup group = getById(groupId);
        if (group == null) {
            throw new BusinessException("小组不存在");
        }
        if (CollectionUtils.isEmpty(userIds)) {
            throw new BusinessException("请选择要拉进小组的人");
        }
        int role = memberRole == null ? ROLE_MEMBER : memberRole;
        for (Long uid : userIds) {
            if (uid == null) {
                continue;
            }
            addMemberRow(groupId, uid, role, JOIN_BY_ADMIN, ST_JOINED);
        }
        log.info("管理员 {} 直接拉 {} 人进入小组 {}，角色 {}", adminId, userIds.size(), groupId, role);
    }

    /**
     * 把「待同意」的记录直接改成「已加入」。管理员拉人用。
     * 如果压根没有记录，addMemberRow 已经建好了，这里只是把状态推过去。
     */
    private void approveIfPending(Long groupId, Long userId, int joinType) {
        BlogGroupMember m = groupMemberMapper.selectOne(new LambdaQueryWrapper<BlogGroupMember>()
                .eq(BlogGroupMember::getGroupId, groupId)
                .eq(BlogGroupMember::getUserId, userId)
                .last("limit 1"));
        if (m == null) {
            return;
        }
        m.setStatus(ST_JOINED);
        m.setJoinType(joinType);
        m.setJoinTime(LocalDateTime.now());
        groupMemberMapper.updateById(m);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleInvite(Long userId, Long memberId, Integer accept) {
        BlogGroupMember m = groupMemberMapper.selectById(memberId);
        if (m == null) {
            throw new BusinessException("邀请记录不存在");
        }
        if (!userId.equals(m.getUserId())) {
            throw new BusinessException("这条邀请不是发给你的");
        }
        if (m.getStatus() == null || m.getStatus() != ST_PENDING) {
            throw new BusinessException("这条邀请已经处理过了");
        }
        if (accept != null && accept == 1) {
            m.setStatus(ST_JOINED);
            m.setJoinTime(LocalDateTime.now());
        } else {
            m.setStatus(ST_REJECTED);
        }
        groupMemberMapper.updateById(m);
        log.info("用户 {} {} 了进入小组 {} 的邀请", userId, (accept != null && accept == 1) ? "同意" : "拒绝",
                m.getGroupId());
    }

    @Override
    public List<GroupMemberVO> listMyPendingInvites(Long userId) {
        List<BlogGroupMember> rows = groupMemberMapper.selectList(new LambdaQueryWrapper<BlogGroupMember>()
                .eq(BlogGroupMember::getUserId, userId)
                .eq(BlogGroupMember::getStatus, ST_PENDING));
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyList();
        }
        return rows.stream().map(this::toMemberVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long operatorId, Long groupId, Long userId) {
        BlogGroup group = getById(groupId);
        if (group == null) {
            throw new BusinessException("小组不存在");
        }
        if (!operatorId.equals(group.getLeaderId())) {
            throw new BusinessException("只有组长可以移除成员");
        }
        if (userId.equals(group.getLeaderId())) {
            throw new BusinessException("组长不能被移除，请先转让组长");
        }
        BlogGroupMember m = groupMemberMapper.selectOne(new LambdaQueryWrapper<BlogGroupMember>()
                .eq(BlogGroupMember::getGroupId, groupId)
                .eq(BlogGroupMember::getUserId, userId)
                .last("limit 1"));
        if (m == null) {
            throw new BusinessException("这个人不在小组里");
        }
        m.setStatus(ST_QUIT);
        groupMemberMapper.updateById(m);
        log.info("组长 {} 把用户 {} 移出了小组 {}", operatorId, userId, groupId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferLeader(Long operatorId, Long groupId, Long newLeaderId) {
        BlogGroup group = getById(groupId);
        if (group == null) {
            throw new BusinessException("小组不存在");
        }
        if (!operatorId.equals(group.getLeaderId())) {
            throw new BusinessException("只有组长可以转让组长");
        }
        BlogGroupMember target = groupMemberMapper.selectOne(new LambdaQueryWrapper<BlogGroupMember>()
                .eq(BlogGroupMember::getGroupId, groupId)
                .eq(BlogGroupMember::getUserId, newLeaderId)
                .eq(BlogGroupMember::getStatus, ST_JOINED)
                .last("limit 1"));
        if (target == null) {
            throw new BusinessException("只能把组长转给已经在组里的成员");
        }
        // 老组长降为普通组员，新组长升上去 —— 两步都要做，别只做一半
        BlogGroupMember old = groupMemberMapper.selectOne(new LambdaQueryWrapper<BlogGroupMember>()
                .eq(BlogGroupMember::getGroupId, groupId)
                .eq(BlogGroupMember::getUserId, operatorId)
                .last("limit 1"));
        if (old != null) {
            old.setMemberRole(ROLE_MEMBER);
            groupMemberMapper.updateById(old);
        }
        target.setMemberRole(ROLE_LEADER);
        groupMemberMapper.updateById(target);

        group.setLeaderId(newLeaderId);
        group.setUpdateBy(operatorId);
        updateById(group);
        log.info("小组 {} 的组长由 {} 转给 {}", groupId, operatorId, newLeaderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindArticle(Long groupId, Long articleId) {
        BlogGroup group = getById(groupId);
        if (group == null) {
            throw new BusinessException("小组不存在");
        }
        if (group.getArticleId() != null && !group.getArticleId().equals(articleId)) {
            throw new BusinessException("这个小组已经绑定过项目了，一个小组只能有一个项目");
        }
        group.setArticleId(articleId);
        updateById(group);
    }

    // ==================== 权限判断（答辩意见的可见性靠这几个方法） ====================

    @Override
    public boolean isGroupMember(Long groupId, Long userId) {
        if (groupId == null || userId == null) {
            return false;
        }
        Long cnt = groupMemberMapper.selectCount(new LambdaQueryWrapper<BlogGroupMember>()
                .eq(BlogGroupMember::getGroupId, groupId)
                .eq(BlogGroupMember::getUserId, userId)
                .eq(BlogGroupMember::getStatus, ST_JOINED));
        return cnt != null && cnt > 0;
    }

    @Override
    public boolean isGroupLeader(Long groupId, Long userId) {
        if (groupId == null || userId == null) {
            return false;
        }
        Long cnt = groupMemberMapper.selectCount(new LambdaQueryWrapper<BlogGroupMember>()
                .eq(BlogGroupMember::getGroupId, groupId)
                .eq(BlogGroupMember::getUserId, userId)
                .eq(BlogGroupMember::getMemberRole, ROLE_LEADER)
                .eq(BlogGroupMember::getStatus, ST_JOINED));
        return cnt != null && cnt > 0;
    }

    @Override
    public List<Long> listMyGroupIds(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<BlogGroupMember> rows = groupMemberMapper.selectList(new LambdaQueryWrapper<BlogGroupMember>()
                .eq(BlogGroupMember::getUserId, userId)
                .eq(BlogGroupMember::getStatus, ST_JOINED));
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyList();
        }
        return rows.stream().map(BlogGroupMember::getGroupId).distinct().collect(Collectors.toList());
    }

    // ==================== 内部工具 ====================

    /**
     * 加一条成员记录。已经存在同组同人的记录时改状态，不新增 ——
     * 表上有 uk_group_user 唯一索引，硬插会报错。
     */
    private void addMemberRow(Long groupId, Long userId, int memberRole, int joinType, int status) {
        BlogGroupMember exist = groupMemberMapper.selectOne(new LambdaQueryWrapper<BlogGroupMember>()
                .eq(BlogGroupMember::getGroupId, groupId)
                .eq(BlogGroupMember::getUserId, userId)
                .last("limit 1"));
        if (exist != null) {
            // 已加入的不动，避免把正式成员又打回待同意
            if (exist.getStatus() != null && exist.getStatus() == ST_JOINED) {
                return;
            }
            exist.setMemberRole(memberRole);
            exist.setJoinType(joinType);
            exist.setStatus(status);
            if (status == ST_JOINED) {
                exist.setJoinTime(LocalDateTime.now());
            }
            groupMemberMapper.updateById(exist);
            return;
        }
        BlogGroupMember m = new BlogGroupMember();
        m.setGroupId(groupId);
        m.setUserId(userId);
        m.setMemberRole(memberRole);
        m.setJoinType(joinType);
        m.setStatus(status);
        if (status == ST_JOINED) {
            m.setJoinTime(LocalDateTime.now());
        }
        groupMemberMapper.insert(m);
    }

    /**
     * 组装小组 VO。withMembers=false 时不查成员列表，用于列表场景避免 N+1。
     */
    private GroupVO toVO(BlogGroup g, Long viewerId, boolean withMembers) {
        GroupVO vo = new GroupVO();
        vo.setId(g.getId());
        vo.setName(g.getName());
        vo.setIntro(g.getIntro());
        vo.setArticleId(g.getArticleId());
        vo.setLeaderId(g.getLeaderId());
        vo.setOpinionScope(g.getOpinionScope());
        vo.setOpinionScopeName(scopeName(g.getOpinionScope()));
        vo.setCreateTime(g.getCreateTime());
        vo.setIsLeader(viewerId != null && viewerId.equals(g.getLeaderId()));
        vo.setIsMember(viewerId != null && isGroupMember(g.getId(), viewerId));
        // 昵称由 admin 层回填（本模块拿不到用户表）
        if (withMembers) {
            List<BlogGroupMember> all = groupMemberMapper.selectList(
                    new LambdaQueryWrapper<BlogGroupMember>()
                            .eq(BlogGroupMember::getGroupId, g.getId()));
            List<GroupMemberVO> joined = new ArrayList<>();
            List<GroupMemberVO> pending = new ArrayList<>();
            for (BlogGroupMember m : all) {
                GroupMemberVO mv = toMemberVO(m);
                if (m.getStatus() != null && m.getStatus() == ST_PENDING) {
                    pending.add(mv);
                } else if (m.getStatus() != null && m.getStatus() == ST_JOINED) {
                    joined.add(mv);
                }
            }
            vo.setMembers(joined);
            vo.setPendingMembers(pending);
        }
        return vo;
    }

    private GroupMemberVO toMemberVO(BlogGroupMember m) {
        GroupMemberVO vo = new GroupMemberVO();
        vo.setId(m.getId());
        vo.setGroupId(m.getGroupId());
        vo.setUserId(m.getUserId());
        vo.setMemberRole(m.getMemberRole());
        vo.setMemberRoleName(memberRoleName(m.getMemberRole()));
        vo.setJoinType(m.getJoinType());
        vo.setJoinTypeName(m.getJoinType() != null && m.getJoinType() == JOIN_BY_ADMIN ? "管理员拉入" : "邀请加入");
        vo.setStatus(m.getStatus());
        vo.setStatusName(statusName(m.getStatus()));
        vo.setJoinTime(m.getJoinTime());
        return vo;
    }

    private String memberRoleName(Integer role) {
        if (role == null) {
            return "成员";
        }
        return switch (role) {
            case ROLE_LEADER -> "组长";
            case ROLE_TEACHER -> "指导老师";
            default -> "组员";
        };
    }

    private String statusName(Integer st) {
        if (st == null) {
            return "未知";
        }
        return switch (st) {
            case ST_PENDING -> "待同意";
            case ST_JOINED -> "已加入";
            case ST_REJECTED -> "已拒绝";
            case ST_QUIT -> "已退出";
            default -> "未知";
        };
    }

    private String scopeName(Integer scope) {
        if (scope == null) {
            return "仅组内可见";
        }
        return switch (scope) {
            case 0 -> "仅组内可见";
            case 2 -> "组外可见全部";
            default -> "组外可见摘要";
        };
    }
}
