package com.qll.ucch.service;

import com.qll.ucch.models.po.BlogRegistration;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qll.ucch.models.dto.CollabProgressDTO;
import com.qll.ucch.models.dto.RegistrationHandleDTO;
import com.qll.ucch.models.dto.RegistrationQueryDTO;
import com.qll.ucch.models.dto.RegistrationSubmitDTO;
import com.qll.ucch.models.vo.RegistrationVO;

/**
 * 对接申请业务接口。
 * 场景：学生申请老师需求 / 老师对学生项目感兴趣，都走这里。
 *
 * @author qll
 */
public interface IBlogRegistrationService extends IService<BlogRegistration> {

    /**
     * 提交对接申请。写 blog_registration，pass/进度都落待处理，并通知文章发布者。
     *
     * 校验按角色分两套：学生必填「年级 + 班级 + 擅长」，老师必填「为什么感兴趣」。
     *
     * @param dto         申请内容
     * @param applicantId 申请人用户ID
     * @param isTeacher   申请人是不是老师（决定用哪套校验口径）
     * @return 申请ID
     */
    Long submitRegistration(RegistrationSubmitDTO dto, Long applicantId, boolean isTeacher);

    /**
     * 对接申请列表：我发出的 / 我收到的。
     * 我发出的按 user_id 筛；我收到的要按「文章是我的」来找，所以先反查我的文章ID。
     *
     * @param query         查询条件
     * @param currentUserId 当前登录用户ID
     * @return 分页结果
     */
    Page<RegistrationVO> pageRegistration(RegistrationQueryDTO query, Long currentUserId);

    /**
     * 对接申请详情。
     *
     * @param id            申请ID
     * @param currentUserId 当前登录用户ID，只有申请人或文章发布者能看
     * @return 详情
     */
    RegistrationVO getRegistrationDetail(Long id, Long currentUserId);

    /**
     * 处理对接申请：通过 / 拒绝。更新 pass、review_message、collab_progress，并通知申请人。
     *
     * @param dto           处理入参
     * @param handlerId     处理人（必须是文章发布者）
     */
    void handleRegistration(RegistrationHandleDTO dto, Long handlerId);

    /**
     * 【新增功能】对接进度更新：collab_progress 状态流转。
     * 申请人和文章发布者双方都能推，但只能按 CollabProgressEnum 的规则往前走。
     *
     * @param dto           进度入参
     * @param operatorId    操作人用户ID
     */
    void updateCollabProgress(CollabProgressDTO dto, Long operatorId);

    /**
     * 撤回自己发出的申请（逻辑删除）。
     *
     * @param id            申请ID
     * @param applicantId   申请人用户ID
     */
    void cancelRegistration(Long id, Long applicantId);

    /**
     * 删除申请（对方删除自己收到的申请记录）。
     *
     * @param id            申请ID
     * @param operatorId    操作人用户ID
     */
    void deleteRegistration(Long id, Long operatorId);

    /**
     * 统计某篇文章收到的申请数（发布者看板用）。
     *
     * @param articleId 文章ID
     * @return 数量
     */
    Long countByArticle(Long articleId);

    /**
     * 统计我收到的待处理申请数（红点提示用）。
     *
     * @param ownerId 文章发布者用户ID
     * @return 数量
     */
    Long countPendingReceived(Long ownerId);
}