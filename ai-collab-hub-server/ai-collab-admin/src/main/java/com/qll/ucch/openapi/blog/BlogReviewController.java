package com.qll.ucch.openapi.blog;

import com.qll.ucch.exception.BusinessException;
import com.qll.ucch.models.PageResult;
import com.qll.ucch.models.Result;
import com.qll.ucch.models.dto.ArticleAuditDTO;
import com.qll.ucch.models.dto.ArticleQueryDTO;
import com.qll.ucch.models.vo.ArticleAuditResultVO;
import com.qll.ucch.models.vo.ArticleAuditVO;
import com.qll.ucch.security.SecurityContext;
import com.qll.ucch.service.IBlogArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 内容审核接口（管理员）。
 * 学生/老师发布的项目、需求、课程都先落待审状态，管理员把过关后才公开。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@RestController
@RequestMapping("/blog/article/review")
@RequiredArgsConstructor
@Tag(name = "内容审核")
public class BlogReviewController {

    private final IBlogArticleService blogArticleService;

    @GetMapping("/list")
    @Operation(summary = "待审核内容列表")
    public Result<PageResult<ArticleAuditVO>> auditList(ArticleQueryDTO query) {
        // 默认只看待审的，前端也可以传 statusId 看历史审核结果
        if (query.getStatusId() == null) {
            query.setStatusId(1L);
        }
        return Result.success(PageResult.of(blogArticleService.pageAuditList(query)));
    }

    @GetMapping("/record/{articleId}")
    @Operation(summary = "查某篇内容的审核记录")
    public Result<List<ArticleAuditVO>> auditRecords(@PathVariable("articleId") Long articleId) {
        return Result.success(blogArticleService.listAuditRecords(articleId));
    }

    @PostMapping
    @Operation(summary = "审核内容（通过 / 拒绝）")
    public Result<Void> audit(@Valid @RequestBody ArticleAuditDTO dto) {
        if (dto.getId() == null && (dto.getIds() == null || dto.getIds().isEmpty())) {
            throw new BusinessException("请指定要审核的内容");
        }
        blogArticleService.auditArticle(dto, SecurityContext.requireUserId());

        String tip = dto.getPass() != null && dto.getPass() == 1 ? "审核通过" : "已拒绝该内容";
        return Result.success(tip, null);
    }

    /**
     * 批量审核。
     * 一条一条调底层单条审核，失败的不影响其它条，返回统计结果让前端提示。
     */
    @PostMapping("/batch")
    @Operation(summary = "批量审核内容")
    public Result<ArticleAuditResultVO> batchAudit(@Valid @RequestBody ArticleAuditDTO dto) {
        if (dto.getIds() == null || dto.getIds().isEmpty()) {
            throw new BusinessException("请至少选择一条内容");
        }
        ArticleAuditResultVO result = blogArticleService.batchAuditArticle(dto,
                SecurityContext.requireUserId());
        return Result.success(result.getMessage(), result);
    }
}
