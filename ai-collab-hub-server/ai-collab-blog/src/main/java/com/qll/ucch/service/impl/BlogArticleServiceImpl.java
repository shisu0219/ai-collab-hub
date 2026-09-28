package com.qll.ucch.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qll.ucch.mapper.BlogArticleInfoMapper;
import com.qll.ucch.mapper.BlogArticleReviewMapper;
import com.qll.ucch.mapper.BlogArticleStatusMapper;
import com.qll.ucch.mapper.BlogArticleTagMapper;
import com.qll.ucch.mapper.BlogDemandExtendMapper;
import com.qll.ucch.mapper.BlogLessonExtendMapper;
import com.qll.ucch.mapper.BlogProgressMapper;
import com.qll.ucch.mapper.BlogProjectExtendMapper;
import com.qll.ucch.mapper.BlogTypeMapper;
import com.qll.ucch.models.common.BusinessException;
import com.qll.ucch.models.dto.ArticleAuditDTO;
import com.qll.ucch.models.dto.ArticleQueryDTO;
import com.qll.ucch.models.dto.ArticleSaveDTO;
import com.qll.ucch.models.enums.ArticleStatusEnum;
import com.qll.ucch.models.enums.ArticleTypeEnum;
import com.qll.ucch.models.enums.NoticeMsgTypeEnum;
import com.qll.ucch.models.po.BlogArticleInfo;
import com.qll.ucch.models.po.BlogArticleReview;
import com.qll.ucch.models.po.BlogArticleStatus;
import com.qll.ucch.models.po.BlogArticleTag;
import com.qll.ucch.models.po.BlogDemandExtend;
import com.qll.ucch.models.po.BlogLessonExtend;
import com.qll.ucch.models.po.BlogProgress;
import com.qll.ucch.models.po.BlogProjectExtend;
import com.qll.ucch.models.po.BlogType;
import com.qll.ucch.models.vo.ArticleAuditResultVO;
import com.qll.ucch.models.vo.ArticleAuditVO;
import com.qll.ucch.models.vo.ArticleDetailVO;
import com.qll.ucch.models.vo.ArticleListVO;
import com.qll.ucch.service.IBlogArticleService;
import com.qll.ucch.service.IUserFavoriteService;
import com.qll.ucch.mapper.BlogGroupMapper;
import com.qll.ucch.models.po.BlogGroup;
import com.qll.ucch.service.INoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文章（合作内容）业务实现。
 *
 * <p>几个约定：
 * <ul>
 *     <li>新发布的内容状态固定落 1（待审核），改内容后也会回落待审核，保证审核不被绕过；</li>
 *     <li>列表接口只查主表 + 字典表凑名称，不连扩展表；详情接口才查扩展表；</li>
 *     <li>通知走 notice 模块的 INoticeService，本模块不自己发消息。</li>
 * </ul>
 *
 * @author qll
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogArticleServiceImpl extends ServiceImpl<BlogArticleInfoMapper, BlogArticleInfo> implements IBlogArticleService {

    private final BlogArticleInfoMapper blogArticleInfoMapper;

    private final BlogArticleReviewMapper blogArticleReviewMapper;

    private final BlogTypeMapper blogTypeMapper;

    private final BlogArticleStatusMapper blogArticleStatusMapper;

    private final BlogArticleTagMapper blogArticleTagMapper;

    private final BlogProgressMapper blogProgressMapper;

    private final BlogProjectExtendMapper blogProjectExtendMapper;

    private final BlogDemandExtendMapper blogDemandExtendMapper;

    private final BlogLessonExtendMapper blogLessonExtendMapper;

    private final IUserFavoriteService userFavoriteService;

    private final INoticeService noticeService;

    /**
     * 小组 Mapper。列表里要显示所属小组名，所以这里直接查一下。
     * 注意：只用来取小组名，不做跨模块的业务判断 —— 那些放 Service 里。
     */
    private final BlogGroupMapper blogGroupMapper;

    // ==========================================================
    // 发布 / 修改 / 删除
    // ==========================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishArticle(ArticleSaveDTO dto, Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException("请先登录");
        }
        if (dto == null || StrUtil.isBlank(dto.getTitle())) {
            throw new BusinessException("标题不能为空");
        }
        ArticleTypeEnum typeEnum = ArticleTypeEnum.of(dto.getTypeId());
        if (typeEnum == null) {
            throw new BusinessException("内容类型不正确");
        }

        BlogArticleInfo po = new BlogArticleInfo();
        po.setUserId(currentUserId);
        // 所属小组：学生填写时必须挂在组下（由上层校验），老师发想法可以不挂
        po.setGroupId(dto.getGroupId());
        // 视频字段本轮不用，但把 DTO 里传的值存上，下一轮直接可用
        po.setVideoUrl(dto.getVideoUrl());
        po.setVideoCover(dto.getVideoCover());
        po.setTitle(dto.getTitle().trim());
        po.setTypeId(dto.getTypeId());
        po.setContent(dto.getContent());
        po.setLocation(dto.getLocation());
        po.setTagId(dto.getTagId());
        po.setProgressId(dto.getProgressId());
        po.setAttachments(dto.getAttachments());
        // 新发的内容统一进待审核，管理员审过后才会对外可见
        po.setStatusId(ArticleStatusEnum.WAIT_AUDIT.getId());
        po.setViewCount(0);

        blogArticleInfoMapper.insert(po);
        Long articleId = po.getId();

        // 按类型写各自的扩展表，一篇文章只对应一张扩展表
        saveExtend(articleId, typeEnum, dto);

        log.info("用户 {} 发布{}内容成功，articleId={}", currentUserId, typeEnum.getName(), articleId);
        return articleId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticle(ArticleSaveDTO dto, Long currentUserId) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("文章ID不能为空");
        }
        if (currentUserId == null) {
            throw new BusinessException("请先登录");
        }

        BlogArticleInfo exist = blogArticleInfoMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException("文章不存在或已被删除");
        }
        // 只有发布者本人能改，管理员走 updateArticleByAdmin
        if (!exist.getUserId().equals(currentUserId)) {
            throw new BusinessException("只能修改自己发布的内容");
        }

        ArticleTypeEnum typeEnum = ArticleTypeEnum.of(dto.getTypeId());
        if (typeEnum == null) {
            throw new BusinessException("内容类型不正确");
        }

        LambdaUpdateWrapper<BlogArticleInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BlogArticleInfo::getId, dto.getId())
                .set(BlogArticleInfo::getTitle, dto.getTitle())
                .set(BlogArticleInfo::getTypeId, dto.getTypeId())
                .set(BlogArticleInfo::getContent, dto.getContent())
                .set(BlogArticleInfo::getLocation, dto.getLocation())
                .set(BlogArticleInfo::getTagId, dto.getTagId())
                .set(BlogArticleInfo::getProgressId, dto.getProgressId())
                .set(BlogArticleInfo::getAttachments, dto.getAttachments())
                // 内容改了就重新排队审核，防止「先发垃圾再审后改」绕过审核
                .set(BlogArticleInfo::getStatusId, ArticleStatusEnum.WAIT_AUDIT.getId());
        blogArticleInfoMapper.update(null, updateWrapper);

        // 类型可能被改过，老的扩展表数据要清掉，再写新的
        removeAllExtend(dto.getId());
        saveExtend(dto.getId(), typeEnum, dto);

        log.info("用户 {} 修改文章成功，articleId={}", currentUserId, dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticleByAdmin(ArticleSaveDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("文章ID不能为空");
        }
        BlogArticleInfo exist = blogArticleInfoMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException("文章不存在或已被删除");
        }
        ArticleTypeEnum typeEnum = ArticleTypeEnum.of(dto.getTypeId());
        if (typeEnum == null) {
            throw new BusinessException("内容类型不正确");
        }

        LambdaUpdateWrapper<BlogArticleInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BlogArticleInfo::getId, dto.getId())
                .set(BlogArticleInfo::getTitle, dto.getTitle())
                .set(BlogArticleInfo::getTypeId, dto.getTypeId())
                .set(BlogArticleInfo::getContent, dto.getContent())
                .set(BlogArticleInfo::getLocation, dto.getLocation())
                .set(BlogArticleInfo::getTagId, dto.getTagId())
                .set(BlogArticleInfo::getProgressId, dto.getProgressId())
                .set(BlogArticleInfo::getAttachments, dto.getAttachments());
        // 管理员改文章不动 status_id，避免把已发布的内容打回审核队列
        blogArticleInfoMapper.update(null, updateWrapper);

        removeAllExtend(dto.getId());
        saveExtend(dto.getId(), typeEnum, dto);

        log.info("管理员修改文章成功，articleId={}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(Long id, Long currentUserId) {
        if (id == null) {
            throw new BusinessException("文章ID不能为空");
        }
        BlogArticleInfo exist = blogArticleInfoMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("文章不存在或已被删除");
        }
        if (currentUserId == null || !exist.getUserId().equals(currentUserId)) {
            throw new BusinessException("只能删除自己发布的内容");
        }

        // 主表走 @TableLogic 逻辑删除，扩展表没有删除字段，直接物理清掉
        blogArticleInfoMapper.deleteById(id);
        removeAllExtend(id);
        log.info("用户 {} 删除文章成功，articleId={}", currentUserId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAttachment(Long articleId, String attachment, Long currentUserId) {
        if (articleId == null || StrUtil.isBlank(attachment)) {
            throw new BusinessException("参数不完整");
        }
        BlogArticleInfo exist = blogArticleInfoMapper.selectById(articleId);
        if (exist == null) {
            throw new BusinessException("文章不存在或已被删除");
        }
        if (currentUserId == null || !exist.getUserId().equals(currentUserId)) {
            throw new BusinessException("只能操作自己发布的内容");
        }

        // 附件是逗号分隔的字符串，删掉指定的那个再写回去
        String remain = removeOneAttachment(exist.getAttachments(), attachment);
        LambdaUpdateWrapper<BlogArticleInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BlogArticleInfo::getId, articleId)
                .set(BlogArticleInfo::getAttachments, remain);
        blogArticleInfoMapper.update(null, updateWrapper);

        log.info("用户 {} 删除文章 {} 的附件 {} 成功", currentUserId, articleId, attachment);
    }

    // ==========================================================
    // 列表 / 详情
    // ==========================================================

    @Override
    public Page<ArticleListVO> pageArticleList(ArticleQueryDTO query) {
        ArticleQueryDTO condition = query == null ? new ArticleQueryDTO() : query;
        long current = normalizePageNum(condition.getPageNum());
        long size = normalizePageSize(condition.getPageSize());

        LambdaQueryWrapper<BlogArticleInfo> wrapper = new LambdaQueryWrapper<>();
        // 新增的搜索能力：标题模糊 + 地区/标签/类型/状态精确过滤，全都可以组合
        wrapper.like(StrUtil.isNotBlank(condition.getTitle()), BlogArticleInfo::getTitle, condition.getTitle())
                .eq(StrUtil.isNotBlank(condition.getLocation()), BlogArticleInfo::getLocation, condition.getLocation())
                .eq(condition.getTagId() != null, BlogArticleInfo::getTagId, condition.getTagId())
                .eq(condition.getTypeId() != null, BlogArticleInfo::getTypeId, condition.getTypeId())
                .eq(condition.getStatusId() != null, BlogArticleInfo::getStatusId, condition.getStatusId())
                .eq(condition.getProgressId() != null, BlogArticleInfo::getProgressId, condition.getProgressId())
                .eq(condition.getUserId() != null, BlogArticleInfo::getUserId, condition.getUserId())
                .orderByDesc(BlogArticleInfo::getCreateTime);

        Page<BlogArticleInfo> poPage = blogArticleInfoMapper.selectPage(new Page<>(current, size), wrapper);
        return convertToListPage(poPage, current, size);
    }

    @Override
    public Page<ArticleListVO> pageMyArticle(ArticleQueryDTO query, Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException("请先登录");
        }
        ArticleQueryDTO condition = query == null ? new ArticleQueryDTO() : query;
        long current = normalizePageNum(condition.getPageNum());
        long size = normalizePageSize(condition.getPageSize());

        LambdaQueryWrapper<BlogArticleInfo> wrapper = new LambdaQueryWrapper<>();
        // 我的发布：先把用户锁死，再叠加筛选条件，防止前端传别人的 userId 把数据查走
        wrapper.eq(BlogArticleInfo::getUserId, currentUserId)
                .like(StrUtil.isNotBlank(condition.getTitle()), BlogArticleInfo::getTitle, condition.getTitle())
                .eq(StrUtil.isNotBlank(condition.getLocation()), BlogArticleInfo::getLocation, condition.getLocation())
                .eq(condition.getTagId() != null, BlogArticleInfo::getTagId, condition.getTagId())
                .eq(condition.getTypeId() != null, BlogArticleInfo::getTypeId, condition.getTypeId())
                .eq(condition.getStatusId() != null, BlogArticleInfo::getStatusId, condition.getStatusId())
                .eq(condition.getProgressId() != null, BlogArticleInfo::getProgressId, condition.getProgressId())
                .orderByDesc(BlogArticleInfo::getCreateTime);

        Page<BlogArticleInfo> poPage = blogArticleInfoMapper.selectPage(new Page<>(current, size), wrapper);
        return convertToListPage(poPage, current, size);
    }

    @Override
    public Page<ArticleListVO> pageArticleByUser(ArticleQueryDTO query) {
        ArticleQueryDTO condition = query == null ? new ArticleQueryDTO() : query;
        if (condition.getUserId() == null) {
            throw new BusinessException("用户ID不能为空");
        }
        long current = normalizePageNum(condition.getPageNum());
        long size = normalizePageSize(condition.getPageSize());

        LambdaQueryWrapper<BlogArticleInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogArticleInfo::getUserId, condition.getUserId())
                // 个人主页是公开展示场景，只给看已发布的
                .eq(BlogArticleInfo::getStatusId, ArticleStatusEnum.PUBLISHED.getId())
                .eq(condition.getTypeId() != null, BlogArticleInfo::getTypeId, condition.getTypeId())
                .eq(condition.getTagId() != null, BlogArticleInfo::getTagId, condition.getTagId())
                .orderByDesc(BlogArticleInfo::getCreateTime);

        Page<BlogArticleInfo> poPage = blogArticleInfoMapper.selectPage(new Page<>(current, size), wrapper);
        return convertToListPage(poPage, current, size);
    }

    @Override
    public List<ArticleListVO> listLatestByUser(Long userId, Integer limit) {
        if (userId == null) {
            return Collections.emptyList();
        }
        int size = limit == null || limit < 1 ? 5 : Math.min(limit, 50);

        LambdaQueryWrapper<BlogArticleInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogArticleInfo::getUserId, userId)
                .eq(BlogArticleInfo::getStatusId, ArticleStatusEnum.PUBLISHED.getId())
                .orderByDesc(BlogArticleInfo::getCreateTime)
                .last("LIMIT " + size);

        List<BlogArticleInfo> list = blogArticleInfoMapper.selectList(wrapper);
        return assembleListVO(list);
    }

    @Override
    public ArticleDetailVO getArticleDetail(Long id, Long currentUserId) {
        if (id == null) {
            throw new BusinessException("文章ID不能为空");
        }
        BlogArticleInfo po = blogArticleInfoMapper.selectById(id);
        if (po == null) {
            throw new BusinessException("内容不存在或已被删除");
        }

        // 浏览量 +1，用 SQL 自增而不是先查后写，避免并发下数字对不上
        LambdaUpdateWrapper<BlogArticleInfo> viewWrapper = new LambdaUpdateWrapper<>();
        viewWrapper.eq(BlogArticleInfo::getId, id)
                .setSql("view_count = IFNULL(view_count, 0) + 1");
        blogArticleInfoMapper.update(null, viewWrapper);

        ArticleDetailVO vo = new ArticleDetailVO();
        vo.setId(po.getId());
        vo.setUserId(po.getUserId());
        vo.setTitle(po.getTitle());
        vo.setTypeId(po.getTypeId());
        vo.setContent(po.getContent());
        vo.setLocation(po.getLocation());
        vo.setTagId(po.getTagId());
        vo.setStatusId(po.getStatusId());
        vo.setProgressId(po.getProgressId());
        vo.setAttachments(po.getAttachments());
        // 返回给前端的数字要跟刚更新完的一致
        vo.setViewCount(po.getViewCount() == null ? 1 : po.getViewCount() + 1);
        vo.setCreateTime(po.getCreateTime());
        vo.setUpdateTime(po.getUpdateTime());

        // 字典名称
        Map<Long, String> typeMap = loadTypeMap();
        Map<Long, String> tagMap = loadTagMap();
        Map<Long, String> statusMap = loadStatusMap();
        Map<Long, String> progressMap = loadProgressMap();
        vo.setTypeName(typeMap.get(po.getTypeId()));
        vo.setTagName(tagMap.get(po.getTagId()));
        vo.setStatusName(statusMap.get(po.getStatusId()));
        vo.setProgressName(progressMap.get(po.getProgressId()));

        // 扩展表信息
        ArticleTypeEnum typeEnum = ArticleTypeEnum.of(po.getTypeId());
        if (typeEnum != null) {
            switch (typeEnum) {
                case PROJECT -> fillProjectExtend(vo, id);
                case DEMAND -> fillDemandExtend(vo, id);
                case LESSON -> fillLessonExtend(vo, id);
            }
        }

        // 发布者信息占位：本模块不查 sys_user，等 admin 层聚合时补
        vo.setPublisherName(null);
        vo.setPublisherAvatar(null);

        // 收藏状态回显
        vo.setFavorited(currentUserId != null
                && userFavoriteService.isFavorited(currentUserId, 1, id));

        return vo;
    }

    // ==========================================================
    // 审核
    // ==========================================================

    @Override
    public Page<ArticleAuditVO> pageAuditList(ArticleQueryDTO query) {
        ArticleQueryDTO condition = query == null ? new ArticleQueryDTO() : query;
        long current = normalizePageNum(condition.getPageNum());
        long size = normalizePageSize(condition.getPageSize());

        LambdaQueryWrapper<BlogArticleInfo> wrapper = new LambdaQueryWrapper<>();
        // 审核台默认只看待审核的，前端也能传 statusId 去看历史
        Long statusId = condition.getStatusId() == null
                ? ArticleStatusEnum.WAIT_AUDIT.getId() : condition.getStatusId();
        wrapper.eq(BlogArticleInfo::getStatusId, statusId)
                .like(StrUtil.isNotBlank(condition.getTitle()), BlogArticleInfo::getTitle, condition.getTitle())
                .eq(condition.getTypeId() != null, BlogArticleInfo::getTypeId, condition.getTypeId())
                .eq(condition.getTagId() != null, BlogArticleInfo::getTagId, condition.getTagId())
                .eq(condition.getUserId() != null, BlogArticleInfo::getUserId, condition.getUserId())
                .orderByAsc(BlogArticleInfo::getCreateTime);

        Page<BlogArticleInfo> poPage = blogArticleInfoMapper.selectPage(new Page<>(current, size), wrapper);

        Map<Long, String> typeMap = loadTypeMap();
        Map<Long, String> tagMap = loadTagMap();
        Map<Long, String> statusMap = loadStatusMap();

        Page<ArticleAuditVO> voPage = new Page<>(current, size, poPage.getTotal());
        List<ArticleAuditVO> records = poPage.getRecords().stream().map(po -> {
            ArticleAuditVO vo = new ArticleAuditVO();
            vo.setArticleId(po.getId());
            vo.setTitle(po.getTitle());
            vo.setTypeId(po.getTypeId());
            vo.setTypeName(typeMap.get(po.getTypeId()));
            vo.setContent(po.getContent());
            vo.setLocation(po.getLocation());
            vo.setTagId(po.getTagId());
            vo.setTagName(tagMap.get(po.getTagId()));
            vo.setStatusId(po.getStatusId());
            vo.setStatusName(statusMap.get(po.getStatusId()));
            vo.setAttachments(po.getAttachments());
            vo.setUserId(po.getUserId());
            vo.setCreateTime(po.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public List<ArticleAuditVO> listAuditRecords(Long articleId) {
        if (articleId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<BlogArticleReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogArticleReview::getArticleId, articleId)
                .orderByDesc(BlogArticleReview::getCreateTime);
        List<BlogArticleReview> reviews = blogArticleReviewMapper.selectList(wrapper);
        if (reviews.isEmpty()) {
            return Collections.emptyList();
        }

        BlogArticleInfo article = blogArticleInfoMapper.selectById(articleId);
        Map<Long, String> statusMap = loadStatusMap();

        return reviews.stream().map(review -> {
            ArticleAuditVO vo = new ArticleAuditVO();
            vo.setReviewId(review.getId());
            vo.setArticleId(review.getArticleId());
            vo.setReviewerId(review.getReviewerId());
            vo.setPass(review.getPass());
            vo.setReason(review.getReason());
            vo.setReviewTime(review.getCreateTime());
            if (article != null) {
                vo.setTitle(article.getTitle());
                vo.setTypeId(article.getTypeId());
                vo.setStatusId(article.getStatusId());
                vo.setStatusName(statusMap.get(article.getStatusId()));
                vo.setUserId(article.getUserId());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditArticle(ArticleAuditDTO dto, Long reviewerId) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("文章ID不能为空");
        }
        if (reviewerId == null) {
            throw new BusinessException("请先登录");
        }
        doAudit(dto.getId(), dto.getPass(), dto.getReason(), reviewerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleAuditResultVO batchAuditArticle(ArticleAuditDTO dto, Long reviewerId) {
        if (dto == null || dto.getIds() == null || dto.getIds().isEmpty()) {
            throw new BusinessException("请选择要审核的内容");
        }
        if (reviewerId == null) {
            throw new BusinessException("请先登录");
        }

        ArticleAuditResultVO result = new ArticleAuditResultVO();
        int success = 0;
        int fail = 0;
        // 同一条记录被审两次没意义，去个重
        Set<Long> idSet = new HashSet<>(dto.getIds());
        for (Long articleId : idSet) {
            try {
                doAudit(articleId, dto.getPass(), dto.getReason(), reviewerId);
                success++;
            } catch (Exception e) {
                // 单条失败不影响整批，把失败的挑出来报告给前端
                fail++;
                log.warn("批量审核文章 {} 失败：{}", articleId, e.getMessage());
            }
        }
        result.setSuccessCount(success);
        result.setFailCount(fail);
        result.setMessage(fail == 0
                ? "全部处理完成"
                : "处理完成，其中 " + fail + " 条跳过（内容不存在或状态已变更）");
        log.info("批量审核完成，审核人={}, 成功={}, 跳过={}", reviewerId, success, fail);
        return result;
    }

    /**
     * 真正干活的审核方法：写审核流水 + 改文章状态 + 通知发布者。
     */
    private void doAudit(Long articleId, Integer pass, String reason, Long reviewerId) {
        if (pass == null) {
            throw new BusinessException("请选择审核结果");
        }
        BlogArticleInfo article = blogArticleInfoMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("内容不存在或已被删除");
        }
        // 只审待审核状态的内容，避免把已发布的反复改来改去
        if (!ArticleStatusEnum.WAIT_AUDIT.getId().equals(article.getStatusId())) {
            throw new BusinessException("该内容当前不是待审核状态");
        }

        BlogArticleReview review = new BlogArticleReview();
        review.setArticleId(articleId);
        review.setReviewerId(reviewerId);
        review.setPass(pass);
        review.setReason(reason);
        blogArticleReviewMapper.insert(review);

        Long targetStatus = pass == 1
                ? ArticleStatusEnum.PUBLISHED.getId()
                : ArticleStatusEnum.REJECTED.getId();
        LambdaUpdateWrapper<BlogArticleInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BlogArticleInfo::getId, articleId)
                .set(BlogArticleInfo::getStatusId, targetStatus)
                .set(BlogArticleInfo::getUpdateBy, reviewerId);
        blogArticleInfoMapper.update(null, updateWrapper);

        // 给发布者发站内信，让他知道审过了还是被拒了
        String title = pass == 1 ? "内容审核通过" : "内容审核未通过";
        StringBuilder content = new StringBuilder();
        content.append("您发布的《").append(article.getTitle()).append("》");
        content.append(pass == 1 ? "已审核通过，现在可以在平台上展示啦。" : "未通过审核。");
        if (StrUtil.isNotBlank(reason)) {
            content.append("审核意见：").append(reason);
        }
        sendNoticeQuietly(article.getUserId(), title, content.toString(), NoticeMsgTypeEnum.AUDIT_RESULT.getCode());

        log.info("审核完成，articleId={}, pass={}, reviewerId={}", articleId, pass, reviewerId);
    }

    // ==========================================================
    // 内部工具方法
    // ==========================================================

    /**
     * 按文章类型写扩展表。有就更新、没有就插入，重复调用不会产生脏数据。
     */
    private void saveExtend(Long articleId, ArticleTypeEnum typeEnum, ArticleSaveDTO dto) {
        switch (typeEnum) {
            case PROJECT -> {
                BlogProjectExtend exist = selectProjectExtend(articleId);
                if (exist == null) {
                    BlogProjectExtend po = new BlogProjectExtend();
                    po.setArticleId(articleId);
                    po.setBudget(dto.getBudget());
                    po.setStartDate(dto.getStartDate());
                    po.setEndDate(dto.getEndDate());
                    blogProjectExtendMapper.insert(po);
                } else {
                    exist.setBudget(dto.getBudget());
                    exist.setStartDate(dto.getStartDate());
                    exist.setEndDate(dto.getEndDate());
                    blogProjectExtendMapper.updateById(exist);
                }
            }
            case DEMAND -> {
                BlogDemandExtend exist = selectDemandExtend(articleId);
                if (exist == null) {
                    BlogDemandExtend po = new BlogDemandExtend();
                    po.setArticleId(articleId);
                    po.setUrgencyLevel(dto.getUrgencyLevel());
                    po.setExpectedDeadline(dto.getExpectedDeadline());
                    blogDemandExtendMapper.insert(po);
                } else {
                    exist.setUrgencyLevel(dto.getUrgencyLevel());
                    exist.setExpectedDeadline(dto.getExpectedDeadline());
                    blogDemandExtendMapper.updateById(exist);
                }
            }
            case LESSON -> {
                BlogLessonExtend exist = selectLessonExtend(articleId);
                if (exist == null) {
                    BlogLessonExtend po = new BlogLessonExtend();
                    po.setArticleId(articleId);
                    po.setMaxStudents(dto.getMaxStudents());
                    // 新建课程报名人数从 0 开始，别让前端传
                    po.setCurrentStudents(0);
                    po.setLessonTime(dto.getLessonTime());
                    po.setLocationDetail(dto.getLocationDetail());
                    blogLessonExtendMapper.insert(po);
                } else {
                    exist.setMaxStudents(dto.getMaxStudents());
                    exist.setLessonTime(dto.getLessonTime());
                    exist.setLocationDetail(dto.getLocationDetail());
                    blogLessonExtendMapper.updateById(exist);
                }
            }
            default -> log.warn("未知的内容类型，跳过扩展表写入，articleId={}", articleId);
        }
    }

    /**
     * 清掉一篇文章的所有扩展记录（改类型或删文章时用）。
     */
    private void removeAllExtend(Long articleId) {
        blogProjectExtendMapper.delete(new LambdaQueryWrapper<BlogProjectExtend>()
                .eq(BlogProjectExtend::getArticleId, articleId));
        blogDemandExtendMapper.delete(new LambdaQueryWrapper<BlogDemandExtend>()
                .eq(BlogDemandExtend::getArticleId, articleId));
        blogLessonExtendMapper.delete(new LambdaQueryWrapper<BlogLessonExtend>()
                .eq(BlogLessonExtend::getArticleId, articleId));
    }

    private BlogProjectExtend selectProjectExtend(Long articleId) {
        return blogProjectExtendMapper.selectOne(new LambdaQueryWrapper<BlogProjectExtend>()
                .eq(BlogProjectExtend::getArticleId, articleId).last("LIMIT 1"));
    }

    private BlogDemandExtend selectDemandExtend(Long articleId) {
        return blogDemandExtendMapper.selectOne(new LambdaQueryWrapper<BlogDemandExtend>()
                .eq(BlogDemandExtend::getArticleId, articleId).last("LIMIT 1"));
    }

    private BlogLessonExtend selectLessonExtend(Long articleId) {
        return blogLessonExtendMapper.selectOne(new LambdaQueryWrapper<BlogLessonExtend>()
                .eq(BlogLessonExtend::getArticleId, articleId).last("LIMIT 1"));
    }

    private void fillProjectExtend(ArticleDetailVO vo, Long articleId) {
        BlogProjectExtend extend = selectProjectExtend(articleId);
        if (extend != null) {
            vo.setBudget(extend.getBudget());
            vo.setStartDate(extend.getStartDate());
            vo.setEndDate(extend.getEndDate());
        }
    }

    private void fillDemandExtend(ArticleDetailVO vo, Long articleId) {
        BlogDemandExtend extend = selectDemandExtend(articleId);
        if (extend != null) {
            vo.setUrgencyLevel(extend.getUrgencyLevel());
            vo.setExpectedDeadline(extend.getExpectedDeadline());
        }
    }

    private void fillLessonExtend(ArticleDetailVO vo, Long articleId) {
        BlogLessonExtend extend = selectLessonExtend(articleId);
        if (extend != null) {
            vo.setMaxStudents(extend.getMaxStudents());
            vo.setCurrentStudents(extend.getCurrentStudents());
            vo.setLessonTime(extend.getLessonTime());
            vo.setLocationDetail(extend.getLocationDetail());
        }
    }

    /**
     * 分页 PO -> 分页 VO，顺便把字典名称填上。
     */
    private Page<ArticleListVO> convertToListPage(Page<BlogArticleInfo> poPage, long current, long size) {
        Page<ArticleListVO> voPage = new Page<>(current, size, poPage.getTotal());
        voPage.setRecords(assembleListVO(poPage.getRecords()));
        return voPage;
    }

    /**
     * 批量转 VO：字典表一次全查出来放 Map，避免每行都去查一次库。
     */
    private List<ArticleListVO> assembleListVO(List<BlogArticleInfo> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> typeMap = loadTypeMap();
        Map<Long, String> tagMap = loadTagMap();
        Map<Long, String> statusMap = loadStatusMap();
        Map<Long, String> progressMap = loadProgressMap();

        List<ArticleListVO> result = new ArrayList<>(list.size());
        for (BlogArticleInfo po : list) {
            ArticleListVO vo = new ArticleListVO();
            vo.setId(po.getId());
            vo.setUserId(po.getUserId());
            vo.setTitle(po.getTitle());
            vo.setTypeId(po.getTypeId());
            vo.setTypeName(typeMap.get(po.getTypeId()));
            vo.setContent(po.getContent());
            vo.setLocation(po.getLocation());
            vo.setTagId(po.getTagId());
            vo.setTagName(tagMap.get(po.getTagId()));
            vo.setStatusId(po.getStatusId());
            vo.setStatusName(statusMap.get(po.getStatusId()));
            vo.setProgressId(po.getProgressId());
            vo.setProgressName(progressMap.get(po.getProgressId()));
            vo.setAttachments(po.getAttachments());
            vo.setViewCount(po.getViewCount());
            vo.setCreateTime(po.getCreateTime());
            // 所属小组（一个组一个项目）
            vo.setGroupId(po.getGroupId());
            result.add(vo);
        }
        // 一次性批量查小组名，别在循环里单条查（那是 N+1）
        fillGroupNamesBatch(result);
        return result;
    }

    /**
     * 批量为列表填小组名。
     * 先收集所有 groupId，一次查出来再回填 —— 循环里单查会变成 N+1。
     */
    private void fillGroupNamesBatch(List<ArticleListVO> list) {
        List<Long> groupIds = list.stream()
                .map(ArticleListVO::getGroupId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (groupIds.isEmpty()) {
            return;
        }
        try {
            List<BlogGroup> groups = blogGroupMapper.selectBatchIds(groupIds);
            Map<Long, String> nameMap = new HashMap<>();
            if (groups != null) {
                for (BlogGroup g : groups) {
                    nameMap.put(g.getId(), g.getName());
                }
            }
            for (ArticleListVO vo : list) {
                if (vo.getGroupId() != null) {
                    vo.setGroupName(nameMap.get(vo.getGroupId()));
                }
            }
        } catch (Exception e) {
            log.warn("回填小组名失败（不影响列表）：{}", e.getMessage());
        }
    }

    private Map<Long, String> loadTypeMap() {
        List<BlogType> list = blogTypeMapper.selectList(new LambdaQueryWrapper<>());
        return toNameMap(list.stream().map(item -> Map.entry(item.getId(), item.getName()))
                .collect(Collectors.toList()));
    }

    private Map<Long, String> loadStatusMap() {
        List<BlogArticleStatus> list = blogArticleStatusMapper.selectList(new LambdaQueryWrapper<>());
        return toNameMap(list.stream().map(item -> Map.entry(item.getId(), item.getName()))
                .collect(Collectors.toList()));
    }

    private Map<Long, String> loadTagMap() {
        List<BlogArticleTag> list = blogArticleTagMapper.selectList(new LambdaQueryWrapper<>());
        return toNameMap(list.stream().map(item -> Map.entry(item.getId(), item.getTagName()))
                .collect(Collectors.toList()));
    }

    private Map<Long, String> loadProgressMap() {
        List<BlogProgress> list = blogProgressMapper.selectList(new LambdaQueryWrapper<>());
        return toNameMap(list.stream().map(item -> Map.entry(item.getId(), item.getName()))
                .collect(Collectors.toList()));
    }

    private Map<Long, String> toNameMap(List<Map.Entry<Long, String>> entries) {
        Map<Long, String> map = new HashMap<>(entries.size());
        for (Map.Entry<Long, String> entry : entries) {
            if (entry.getKey() != null) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    /**
     * 从逗号分隔的附件串里去掉指定的一项。
     */
    private String removeOneAttachment(String attachments, String target) {
        if (StrUtil.isBlank(attachments)) {
            return attachments;
        }
        List<String> remain = Arrays.stream(attachments.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty() && !item.equals(target.trim()))
                .collect(Collectors.toList());
        return remain.isEmpty() ? null : String.join(",", remain);
    }

    /**
     * 发消息通知。通知只是附加动作，失败不应该把主业务回滚掉，所以这里吞掉异常。
     */
    private void sendNoticeQuietly(Long userId, String title, String content, Integer msgType) {
        if (userId == null) {
            return;
        }
        try {
            noticeService.sendNotice(userId, title, content, msgType);
        } catch (Exception e) {
            log.warn("发送站内信失败，userId={}, title={}, 原因：{}", userId, title, e.getMessage());
        }
    }

    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        // 限制一下上限，防止前端传个 100000 把库拖垮
        return Math.min(pageSize, 100L);
    }
}