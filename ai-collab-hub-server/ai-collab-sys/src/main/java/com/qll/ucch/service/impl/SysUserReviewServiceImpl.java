package com.qll.ucch.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qll.ucch.constant.SysConstants;
import com.qll.ucch.exception.SysBizException;
import com.qll.ucch.mapper.SysUserReviewMapper;
import com.qll.ucch.models.dto.UserAuditDTO;
import com.qll.ucch.models.dto.UserReviewPageQueryDTO;
import com.qll.ucch.models.po.SysRole;
import com.qll.ucch.models.po.SysUser;
import com.qll.ucch.models.po.SysUserReview;
import com.qll.ucch.models.vo.UserReviewVO;
import com.qll.ucch.service.SysRoleService;
import com.qll.ucch.service.SysUserReviewService;
import com.qll.ucch.service.SysUserRoleService;
import com.qll.ucch.service.SysUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户审核业务实现。
 * <p>
 * 流程：注册时写一条 audit_status=0 的待审记录 -> 管理员在这里通过/拒绝 ->
 * 回填 reviewer_id、review_time、reason，同时同步 sys_user.audit_status。
 */
@Service
public class SysUserReviewServiceImpl extends ServiceImpl<SysUserReviewMapper, SysUserReview>
        implements SysUserReviewService {

    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final SysUserRoleService sysUserRoleService;

    public SysUserReviewServiceImpl(@Lazy SysUserService sysUserService,
                                    @Lazy SysRoleService sysRoleService,
                                    @Lazy SysUserRoleService sysUserRoleService) {
        this.sysUserService = sysUserService;
        this.sysRoleService = sysRoleService;
        this.sysUserRoleService = sysUserRoleService;
    }

    @Override
    public IPage<UserReviewVO> pageReviews(UserReviewPageQueryDTO query) {
        UserReviewPageQueryDTO cond = query == null ? new UserReviewPageQueryDTO() : query;
        long current = cond.getCurrent() == null || cond.getCurrent() < 1 ? 1L : cond.getCurrent();
        long size = cond.getSize() == null || cond.getSize() < 1 ? 10L : cond.getSize();

        LambdaQueryWrapper<SysUserReview> wrapper = new LambdaQueryWrapper<>();
        // 不传就默认只看待审核的，管理员打开页面第一眼就是待办
        Integer auditStatus = cond.getAuditStatus() == null ? SysConstants.AUDIT_PENDING : cond.getAuditStatus();
        wrapper.eq(SysUserReview::getAuditStatus, auditStatus);

        // 按角色筛
        Long roleId = cond.getRoleId();
        if (roleId == null && StrUtil.isNotBlank(cond.getRoleCode())) {
            SysRole role = sysRoleService.getByCode(cond.getRoleCode());
            if (role == null) {
                // 角色标识都没对上，肯定没结果，直接返回空页
                return new Page<>(current, size);
            }
            roleId = role.getId();
        }
        if (roleId != null) {
            wrapper.eq(SysUserReview::getRoleId, roleId);
        }

        // 按账号 / 昵称筛，这两个字段在 sys_user 上，先查出来过滤成 userId 再 in 进去
        if (StrUtil.isNotBlank(cond.getAccount()) || StrUtil.isNotBlank(cond.getNickname())) {
            LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
            if (StrUtil.isNotBlank(cond.getAccount())) {
                userWrapper.like(SysUser::getAccount, cond.getAccount().trim());
            }
            if (StrUtil.isNotBlank(cond.getNickname())) {
                userWrapper.like(SysUser::getNickname, cond.getNickname().trim());
            }
            List<SysUser> users = sysUserService.list(userWrapper);
            if (CollUtil.isEmpty(users)) {
                return new Page<>(current, size);
            }
            List<Long> userIds = users.stream().map(SysUser::getId).collect(Collectors.toList());
            wrapper.in(SysUserReview::getUserId, userIds);
        }

        wrapper.orderByAsc(SysUserReview::getAuditStatus)
                .orderByDesc(SysUserReview::getCreateTime);

        IPage<SysUserReview> page = page(new Page<>(current, size), wrapper);
        return assemblePage(page);
    }

    @Override
    public SysUserReview getLatestByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<SysUserReview>()
                .eq(SysUserReview::getUserId, userId)
                .orderByDesc(SysUserReview::getCreateTime)
                .last("limit 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(UserAuditDTO dto, Long reviewerId) {
        SysUserReview review = resolveReview(dto);
        SysUser user = sysUserService.getById(review.getUserId());
        if (user == null) {
            throw new SysBizException("被审核的用户不存在");
        }
        if (Objects.equals(user.getAuditStatus(), SysConstants.AUDIT_PASSED)) {
            throw new SysBizException("该用户已经审核通过了，不用重复审核");
        }

        fillReview(review, SysConstants.AUDIT_PASSED, reviewerId,
                StrUtil.isBlank(dto.getReason()) ? "审核通过" : dto.getReason());
        updateById(review);

        // 同步用户表状态
        sysUserService.updateAuditStatus(review.getUserId(), SysConstants.AUDIT_PASSED, reviewerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(UserAuditDTO dto, Long reviewerId) {
        if (StrUtil.isBlank(dto.getReason())) {
            throw new SysBizException("拒绝原因不能为空");
        }
        SysUserReview review = resolveReview(dto);
        SysUser user = sysUserService.getById(review.getUserId());
        if (user == null) {
            throw new SysBizException("被审核的用户不存在");
        }
        if (Objects.equals(user.getAuditStatus(), SysConstants.AUDIT_PASSED)) {
            throw new SysBizException("该用户已经审核通过了，不能再改成拒绝");
        }

        fillReview(review, SysConstants.AUDIT_REJECTED, reviewerId, dto.getReason());
        updateById(review);

        sysUserService.updateAuditStatus(review.getUserId(), SysConstants.AUDIT_REJECTED, reviewerId);
    }

    @Override
    public void createPendingReview(Long userId, Long roleId, String credentials) {
        if (userId == null || roleId == null) {
            throw new SysBizException("用户ID和角色ID不能为空");
        }
        SysUserReview review = new SysUserReview();
        review.setUserId(userId);
        review.setRoleId(roleId);
        review.setAuditStatus(SysConstants.AUDIT_PENDING);
        review.setCredentials(credentials);
        save(review);
    }

    // ==================== 私有方法 ====================

    /**
     * 定位要审核的那条记录：优先用传进来的 reviewId，
     * 没传就按 userId 找最新的那条。
     */
    private SysUserReview resolveReview(UserAuditDTO dto) {
        if (dto == null || dto.getUserId() == null) {
            throw new SysBizException("用户ID不能为空");
        }
        SysUserReview review = null;
        if (dto.getReviewId() != null) {
            review = getById(dto.getReviewId());
            if (review == null) {
                throw new SysBizException("审核记录不存在");
            }
        } else {
            review = getLatestByUserId(dto.getUserId());
            if (review == null) {
                throw new SysBizException("该用户没有待审核的记录");
            }
        }
        if (!Objects.equals(review.getUserId(), dto.getUserId())) {
            throw new SysBizException("审核记录和用户对不上，请刷新后重试");
        }
        return review;
    }

    /**
     * 把审核结果写到记录上
     */
    private void fillReview(SysUserReview review, Integer auditStatus, Long reviewerId, String reason) {
        review.setAuditStatus(auditStatus);
        review.setReviewerId(reviewerId);
        review.setReviewTime(LocalDateTime.now());
        review.setReason(reason);
    }

    /**
     * 分页结果转 VO。这里一次性把用户、角色都捞出来，避免一条一条查（N+1）。
     */
    private IPage<UserReviewVO> assemblePage(IPage<SysUserReview> page) {
        List<UserReviewVO> records = new ArrayList<>();
        List<SysUserReview> list = page.getRecords();
        if (CollUtil.isNotEmpty(list)) {
            List<Long> userIds = list.stream()
                    .map(SysUserReview::getUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            Map<Long, SysUser> userMap = new HashMap<>();
            if (CollUtil.isNotEmpty(userIds)) {
                for (SysUser user : sysUserService.listByIds(userIds)) {
                    userMap.put(user.getId(), user);
                }
            }

            Map<Long, SysRole> roleMap = new HashMap<>();
            List<Long> roleIds = list.stream()
                    .map(SysUserReview::getRoleId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (CollUtil.isNotEmpty(roleIds)) {
                for (SysRole role : sysRoleService.listByIds(roleIds)) {
                    roleMap.put(role.getId(), role);
                }
            }

            // 审核人也可能是管理员，列表上顺手把昵称带出来
            Map<Long, SysUser> reviewerMap = new HashMap<>();
            List<Long> reviewerIds = list.stream()
                    .map(SysUserReview::getReviewerId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (CollUtil.isNotEmpty(reviewerIds)) {
                for (SysUser reviewer : sysUserService.listByIds(reviewerIds)) {
                    reviewerMap.put(reviewer.getId(), reviewer);
                }
            }

            for (SysUserReview review : list) {
                records.add(toVO(review, userMap, roleMap, reviewerMap));
            }
        }

        Page<UserReviewVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(records);
        return result;
    }

    private UserReviewVO toVO(SysUserReview review,
                              Map<Long, SysUser> userMap,
                              Map<Long, SysRole> roleMap,
                              Map<Long, SysUser> reviewerMap) {
        UserReviewVO vo = new UserReviewVO();
        BeanUtils.copyProperties(review, vo);

        SysUser user = userMap.get(review.getUserId());
        if (user != null) {
            vo.setAccount(user.getAccount());
            vo.setNickname(user.getNickname());
            vo.setEmail(user.getEmail());
            vo.setPhone(user.getPhone());
            vo.setAvatar(user.getAvatar());
        }

        SysRole role = roleMap.get(review.getRoleId());
        if (role != null) {
            vo.setRoleCode(role.getCode());
            vo.setRoleName(role.getName());
        }

        SysUser reviewer = reviewerMap.get(review.getReviewerId());
        if (reviewer != null) {
            vo.setReviewerName(reviewer.getNickname());
        }

        vo.setCredentialList(splitCredentials(review.getCredentials()));
        return vo;
    }

    /**
     * 证明材料是按英文逗号拼起来的字符串，拆成列表给前端渲染
     */
    private List<String> splitCredentials(String credentials) {
        if (StrUtil.isBlank(credentials)) {
            return Collections.emptyList();
        }
        return Arrays.stream(credentials.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }
}
