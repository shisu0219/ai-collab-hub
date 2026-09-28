package com.qll.ucch.openapi.user;

import com.qll.ucch.exception.BusinessException;
import com.qll.ucch.models.PageResult;
import com.qll.ucch.models.Result;
import com.qll.ucch.models.dto.UserAuditDTO;
import com.qll.ucch.models.dto.UserReviewPageQueryDTO;
import com.qll.ucch.models.vo.UserReviewVO;
import com.qll.ucch.security.SecurityContext;
import com.qll.ucch.service.SysUserReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.util.List;

/**
 * 用户审核接口（管理员）。
 * 学生 / 老师注册完都是待审状态，管理员在这里看材料、决定放不放行。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@RestController
@RequestMapping("/user/review")
@RequiredArgsConstructor
@Tag(name = "用户审核")
public class UserReviewController {

    private final SysUserReviewService sysUserReviewService;

    @GetMapping("/list")
    @Operation(summary = "待审核用户列表")
    public Result<PageResult<UserReviewVO>> reviewList(@Valid UserReviewPageQueryDTO query) {
        return Result.success(PageResult.of(sysUserReviewService.pageReviews(query)));
    }

    /**
     * 单条审核。
     * 通过和拒绝走的是同一个入口，靠 pass 区分，前端一个按钮组就能搞定。
     */
    @PostMapping
    @Operation(summary = "审核用户（通过 / 拒绝）")
    public Result<Void> review(@Valid @RequestBody UserReviewRequest request) {
        Long reviewerId = SecurityContext.requireUserId();

        UserAuditDTO dto = new UserAuditDTO();
        dto.setUserId(request.getUserId());
        dto.setReviewId(request.getReviewId());
        dto.setReason(request.getReason());

        if (isPass(request.getPass())) {
            sysUserReviewService.approve(dto, reviewerId);
            return Result.success("已通过该账号的申请", null);
        }

        // 拒绝必须写原因，不然用户收到通知也不知道哪里不合格
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new BusinessException("拒绝时必须填写审核意见");
        }
        sysUserReviewService.reject(dto, reviewerId);
        return Result.success("已拒绝该账号的申请", null);
    }

    /**
     * 批量审核。
     * 挨个调 service，某一条失败不拖累其它人，最后把成功条数报回去。
     */
    @PostMapping("/batch")
    @Operation(summary = "批量审核用户")
    public Result<BatchReviewResult> reviewBatch(@Valid @RequestBody UserReviewBatchRequest request) {
        Long reviewerId = SecurityContext.requireUserId();
        List<Long> userIds = request.getUserIds();
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException("请至少选择一个待审核用户");
        }
        boolean pass = isPass(request.getPass());
        if (!pass && (request.getReason() == null || request.getReason().isBlank())) {
            throw new BusinessException("批量拒绝时必须填写审核意见");
        }

        BatchReviewResult result = new BatchReviewResult();
        for (Long userId : userIds) {
            UserAuditDTO dto = new UserAuditDTO();
            dto.setUserId(userId);
            dto.setReason(request.getReason());
            try {
                if (pass) {
                    sysUserReviewService.approve(dto, reviewerId);
                } else {
                    sysUserReviewService.reject(dto, reviewerId);
                }
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                // 单条异常不往外抛，记下来告诉前端哪个人没处理成功
                log.warn("批量审核用户 {} 失败：{}", userId, e.getMessage());
                result.setFailCount(result.getFailCount() + 1);
                result.getFailUserIds().add(userId);
            }
        }
        result.setMessage("成功 " + result.getSuccessCount() + " 条，失败 "
                + result.getFailCount() + " 条");
        return Result.success(result.getMessage(), result);
    }

    /** pass=1 算通过，其它值都当拒绝 */
    private boolean isPass(Integer pass) {
        return pass != null && pass == 1;
    }

    // ==================== 入参 / 出参 ====================

    /**
     * 单条审核入参。
     * 复用 service 的 UserAuditDTO 只有 userId / reviewId / reason 三个字段，
     * 这里多一个 pass 决定走通过还是拒绝。
     */
    @Data
    public static class UserReviewRequest implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long userId;

        private Long reviewId;

        /** 1 通过 0 拒绝 */
        private Integer pass;

        private String reason;
    }

    /** 批量审核入参 */
    @Data
    public static class UserReviewBatchRequest implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 被审核的用户ID列表 */
        private List<Long> userIds;

        /** 1 通过 0 拒绝 */
        private Integer pass;

        /** 审核意见，批量统一生效 */
        private String reason;
    }

    /** 批量审核结果 */
    @Data
    public static class BatchReviewResult implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer successCount = 0;

        private Integer failCount = 0;

        private java.util.List<Long> failUserIds = new java.util.ArrayList<>();

        private String message;
    }
}
