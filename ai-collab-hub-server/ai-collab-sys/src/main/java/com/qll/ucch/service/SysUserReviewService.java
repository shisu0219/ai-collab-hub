package com.qll.ucch.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qll.ucch.models.dto.UserAuditDTO;
import com.qll.ucch.models.dto.UserReviewPageQueryDTO;
import com.qll.ucch.models.po.SysUserReview;
import com.qll.ucch.models.vo.UserReviewVO;

/**
 * 用户审核业务接口。
 * 学生 / 老师注册后处于待审状态，管理员在这里通过或拒绝。
 */
public interface SysUserReviewService extends IService<SysUserReview> {

    /**
     * 分页查询审核记录（默认查待审核的）
     */
    IPage<UserReviewVO> pageReviews(UserReviewPageQueryDTO query);

    /**
     * 根据用户ID查最新一条审核记录
     */
    SysUserReview getLatestByUserId(Long userId);

    /**
     * 审核通过。会写 sys_user_review（result=1、reviewer_id、review_time），
     * 并把 sys_user.audit_status 置为 1。
     *
     * @param dto        被审用户 + 可选的意见
     * @param reviewerId 当前登录管理员ID
     */
    void approve(UserAuditDTO dto, Long reviewerId);

    /**
     * 审核拒绝。reviewer_id / review_time 一起落库，sys_user.audit_status 置为 2。
     * 拒绝时 reason 必填，前端一般从拒绝理由模板里选。
     *
     * @param dto        被审用户 + 拒绝原因
     * @param reviewerId 当前登录管理员ID
     */
    void reject(UserAuditDTO dto, Long reviewerId);

    /**
     * 注册成功后生成一条待审记录（供 SysUserService 注册流程调用）
     */
    void createPendingReview(Long userId, Long roleId, String credentials);
}
