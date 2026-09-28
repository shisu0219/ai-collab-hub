package com.qll.ucch.service;

import com.qll.ucch.models.po.BlogArticleInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qll.ucch.models.dto.ArticleAuditDTO;
import com.qll.ucch.models.dto.ArticleQueryDTO;
import com.qll.ucch.models.dto.ArticleSaveDTO;
import com.qll.ucch.models.vo.ArticleAuditResultVO;
import com.qll.ucch.models.vo.ArticleAuditVO;
import com.qll.ucch.models.vo.ArticleDetailVO;
import com.qll.ucch.models.vo.ArticleListVO;

import java.util.List;

/**
 * 文章（合作内容）业务接口。
 * 覆盖：发布、列表、详情、我的发布、按用户查、修改、删除、审核、批量审核、附件删除。
 *
 * @author qll
 */
public interface IBlogArticleService extends IService<BlogArticleInfo> {

    /**
     * 发布文章。写 blog_article_info + 对应扩展表，status_id 落 1（待审核）。
     *
     * @param dto           文章内容
     * @param currentUserId 当前登录用户ID
     * @return 新文章ID
     */
    Long publishArticle(ArticleSaveDTO dto, Long currentUserId);

    /**
     * 文章列表（简要信息，分页）。支持 title 模糊、location、tagId、typeId、statusId 组合过滤。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    Page<ArticleListVO> pageArticleList(ArticleQueryDTO query);

    /**
     * 文章详情。顺带把浏览量 +1，并填充扩展表信息。
     *
     * @param id            文章ID
     * @param currentUserId 当前登录用户ID，可为 null（未登录只做浏览）
     * @return 详情
     */
    ArticleDetailVO getArticleDetail(Long id, Long currentUserId);

    /**
     * 个人文章列表（我的发布）。默认按当前用户过滤，支持 statusId/progressId/title/location/tagId/typeId。
     *
     * @param query         查询条件
     * @param currentUserId 当前登录用户ID
     * @return 分页结果
     */
    Page<ArticleListVO> pageMyArticle(ArticleQueryDTO query, Long currentUserId);

    /**
     * 按用户ID查该用户的文章列表（个人主页聚合用，新增功能）。
     * 只看已发布的，避免把别人待审核/被拒的内容暴露出去。
     *
     * @param query 查询条件，userId 必传
     * @return 分页结果
     */
    Page<ArticleListVO> pageArticleByUser(ArticleQueryDTO query);

    /**
     * 按用户ID查最新几篇（个人主页顶部展示用）。
     *
     * @param userId 用户ID
     * @param limit  条数
     * @return 列表
     */
    List<ArticleListVO> listLatestByUser(Long userId, Integer limit);

    /**
     * 修改文章（发布者本人）。改了内容要重新走审核，status_id 回落待审核。
     *
     * @param dto           文章内容
     * @param currentUserId 当前登录用户ID
     */
    void updateArticle(ArticleSaveDTO dto, Long currentUserId);

    /**
     * 管理员改文章，不校验归属，也不重置审核状态。
     *
     * @param dto 文章内容
     */
    void updateArticleByAdmin(ArticleSaveDTO dto);

    /**
     * 逻辑删除文章（发布者本人），扩展表一起删掉。
     *
     * @param id            文章ID
     * @param currentUserId 当前登录用户ID
     */
    void deleteArticle(Long id, Long currentUserId);

    /**
     * 删除文章附件（只清 attachments 字段，不动磁盘文件——文件清理交给 integration 模块）。
     *
     * @param articleId    文章ID
     * @param attachment   要删的附件地址
     * @param currentUserId 当前登录用户ID
     */
    void deleteAttachment(Long articleId, String attachment, Long currentUserId);

    /**
     * 待审核内容列表（管理员）。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    Page<ArticleAuditVO> pageAuditList(ArticleQueryDTO query);

    /**
     * 查询某篇文章的审核记录。
     *
     * @param articleId 文章ID
     * @return 审核记录列表，按时间倒序
     */
    List<ArticleAuditVO> listAuditRecords(Long articleId);

    /**
     * 内容审核（单条）：写 blog_article_review + 更新 article.status_id，并给发布者发站内信。
     *
     * @param dto        审核入参
     * @param reviewerId 审核人ID
     */
    void auditArticle(ArticleAuditDTO dto, Long reviewerId);

    /**
     * 批量审核（新增功能）：按ID列表批量通过/拒绝。
     *
     * @param dto        审核入参，ids 必传
     * @param reviewerId 审核人ID
     * @return 处理结果统计
     */
    ArticleAuditResultVO batchAuditArticle(ArticleAuditDTO dto, Long reviewerId);
}