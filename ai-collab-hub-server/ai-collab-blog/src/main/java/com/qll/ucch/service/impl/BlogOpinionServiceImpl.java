package com.qll.ucch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qll.ucch.exception.BusinessException;
import com.qll.ucch.mapper.BlogGroupMemberMapper;
import com.qll.ucch.mapper.BlogOpinionMapper;
import com.qll.ucch.models.dto.OpinionImportRowDTO;
import com.qll.ucch.models.dto.OpinionSaveDTO;
import com.qll.ucch.models.po.BlogGroup;
import com.qll.ucch.models.po.BlogGroupMember;
import com.qll.ucch.models.po.BlogOpinion;
import com.qll.ucch.models.vo.OpinionVO;
import com.qll.ucch.service.IBlogGroupService;
import com.qll.ucch.service.IBlogOpinionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 答辩意见服务实现。
 *
 * 【可见性在这里实现，这是整个模块最要紧的逻辑】
 *
 * 组内（组长/组员/指导老师）和管理员 -> 全部可见。
 * 组外 -> 逐条算 effectiveScope，再决定返回什么：
 *   0 = 仅组内  -> 这条直接不返回
 *   1 = 可见摘要 -> 返回标题+来源+截断正文，truncated=true
 *   2 = 可见全部 -> 原样返回
 *
 * 过滤写在 Service 而不是 Controller，是因为列表和详情两个入口都要过同一套规则，
 * 放一处才不会漏。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogOpinionServiceImpl extends ServiceImpl<BlogOpinionMapper, BlogOpinion>
        implements IBlogOpinionService {

    /** 小组的默认可见性：可见摘要 */
    public static final int SCOPE_INNER_ONLY = 0;
    public static final int SCOPE_SUMMARY = 1;
    public static final int SCOPE_ALL = 2;

    /** 摘要模式下截断的字数 */
    private static final int SUMMARY_LENGTH = 50;

    private final IBlogGroupService blogGroupService;
    private final BlogGroupMemberMapper groupMemberMapper;

    // ==================== 查询（带可见性裁剪） ====================

    @Override
    public IPage<OpinionVO> pageByGroup(Long groupId, Long viewerId, boolean isAdmin,
                                        Long pageNum, Long pageSize) {
        if (groupId == null) {
            throw new BusinessException("小组ID不能为空");
        }
        BlogGroup group = blogGroupService.getById(groupId);
        if (group == null) {
            throw new BusinessException("小组不存在");
        }
        boolean insider = isAdmin || blogGroupService.isGroupMember(groupId, viewerId);

        Page<BlogOpinion> page = new Page<>(normPage(pageNum), normSize(pageSize));
        IPage<BlogOpinion> raw = page(page, new LambdaQueryWrapper<BlogOpinion>()
                .eq(BlogOpinion::getGroupId, groupId)
                .orderByDesc(BlogOpinion::getCreateTime));

        return convert(raw, group, insider);
    }

    @Override
    public IPage<OpinionVO> pageByArticle(Long articleId, Long viewerId, boolean isAdmin,
                                          Long pageNum, Long pageSize) {
        if (articleId == null) {
            throw new BusinessException("项目ID不能为空");
        }
        BlogGroup group = null;
        if (blogGroupService != null) {
            group = blogGroupService.getOne(new LambdaQueryWrapper<BlogGroup>()
                    .eq(BlogGroup::getArticleId, articleId)
                    .eq(BlogGroup::getStatus, 1)
                    .last("limit 1"));
        }
        boolean insider = isAdmin || (group != null && blogGroupService.isGroupMember(group.getId(), viewerId));

        Page<BlogOpinion> page = new Page<>(normPage(pageNum), normSize(pageSize));
        IPage<BlogOpinion> raw = page(page, new LambdaQueryWrapper<BlogOpinion>()
                .eq(BlogOpinion::getArticleId, articleId)
                .orderByDesc(BlogOpinion::getCreateTime));

        return convert(raw, group, insider);
    }

    @Override
    public OpinionVO getOpinionDetail(Long opinionId, Long viewerId, boolean isAdmin) {
        BlogOpinion op = getById(opinionId);
        if (op == null) {
            throw new BusinessException("意见不存在或已被删除");
        }
        BlogGroup group = blogGroupService.getById(op.getGroupId());
        boolean insider = isAdmin || blogGroupService.isGroupMember(op.getGroupId(), viewerId);
        int effective = resolveScope(op, group);

        // 组外 + 仅组内 -> 就是不给看，直接拒绝，别返回半截数据
        if (!insider && effective == SCOPE_INNER_ONLY) {
            throw new BusinessException("这条答辩意见仅小组内可见");
        }
        return toVO(op, group, insider, effective);
    }

    @Override
    public int countVisible(Long groupId, Long viewerId, boolean isAdmin) {
        BlogGroup group = blogGroupService.getById(groupId);
        if (group == null) {
            return 0;
        }
        boolean insider = isAdmin || blogGroupService.isGroupMember(groupId, viewerId);
        List<BlogOpinion> all = list(new LambdaQueryWrapper<BlogOpinion>()
                .eq(BlogOpinion::getGroupId, groupId));
        if (insider) {
            return all.size();
        }
        // 组外：只数可见的
        int n = 0;
        for (BlogOpinion op : all) {
            if (resolveScope(op, group) != SCOPE_INNER_ONLY) {
                n++;
            }
        }
        return n;
    }

    // ==================== 写入 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveOpinion(Long adminId, OpinionSaveDTO dto) {
        // 正文和附件至少有一个 —— 答辩资料常常只有一张表格，正文空着也正常，
        // 所以这个校验只能在 Service 里做，不能靠 DTO 上的 @NotBlank
        boolean hasText = StringUtils.hasText(dto.getContent());
        boolean hasFile = !CollectionUtils.isEmpty(dto.getAttachments());
        if (!hasText && !hasFile) {
            throw new BusinessException("请填写意见内容或上传答辩资料");
        }
        if (dto.getGroupId() == null) {
            throw new BusinessException("请选择所属小组");
        }
        BlogGroup group = blogGroupService.getById(dto.getGroupId());
        if (group == null) {
            throw new BusinessException("小组不存在");
        }

        BlogOpinion op;
        if (dto.getId() != null) {
            op = getById(dto.getId());
            if (op == null) {
                throw new BusinessException("意见不存在");
            }
        } else {
            op = new BlogOpinion();
            op.setGroupId(dto.getGroupId());
            op.setUploaderId(adminId);
            op.setCreateBy(adminId);
        }
        op.setArticleId(dto.getArticleId() != null ? dto.getArticleId() : group.getArticleId());
        op.setTitle(dto.getTitle());
        op.setContent(dto.getContent());
        op.setOpinionType(dto.getOpinionType() == null ? 1 : dto.getOpinionType());
        op.setSource(dto.getSource());
        if (dto.getAttachments() != null) {
            op.setAttachments(String.join(",", dto.getAttachments()));
        }
        op.setScopeOverride(dto.getScopeOverride());
        op.setUpdateBy(adminId);

        saveOrUpdate(op);
        log.info("管理员 {} 保存答辩意见 {}（小组 {}）", adminId, op.getId(), op.getGroupId());
        return op.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importOpinions(Long adminId, Long groupId, List<OpinionImportRowDTO> rows) {
        if (groupId == null) {
            throw new BusinessException("请选择所属小组");
        }
        if (CollectionUtils.isEmpty(rows)) {
            throw new BusinessException("没有解析到可导入的数据");
        }
        BlogGroup group = blogGroupService.getById(groupId);
        if (group == null) {
            throw new BusinessException("小组不存在");
        }
        int ok = 0;
        for (OpinionImportRowDTO row : rows) {
            // 整行都空的就跳过（Excel 尾部的空行很常见）
            if (!StringUtils.hasText(row.getTitle()) && !StringUtils.hasText(row.getContent())) {
                continue;
            }
            BlogOpinion op = new BlogOpinion();
            op.setGroupId(groupId);
            op.setArticleId(group.getArticleId());
            op.setTitle(row.getTitle());
            op.setContent(row.getContent());
            op.setOpinionType(row.getOpinionType() == null ? 1 : row.getOpinionType());
            op.setSource(row.getSource());
            op.setUploaderId(adminId);
            op.setCreateBy(adminId);
            op.setUpdateBy(adminId);
            // 批量导入的默认跟随小组全局设置，单条覆盖留给导入后手动调
            op.setScopeOverride(null);
            save(op);
            ok++;
        }
        log.info("管理员 {} 批量导入答辩意见 {} 条到小组 {}", adminId, ok, groupId);
        return ok;
    }

    @Override
    public void deleteOpinion(Long adminId, Long opinionId) {
        BlogOpinion op = getById(opinionId);
        if (op == null) {
            throw new BusinessException("意见不存在");
        }
        removeById(op);
        log.info("管理员 {} 删除答辩意见 {}", adminId, opinionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setScope(Long operatorId, Long groupId, Long opinionId, Integer scope) {
        BlogGroup group = blogGroupService.getById(groupId);
        if (group == null) {
            throw new BusinessException("小组不存在");
        }
        // 只有组长能设置可见范围
        if (!blogGroupService.isGroupLeader(groupId, operatorId)) {
            throw new BusinessException("只有组长可以设置答辩意见的可见范围");
        }
        if (opinionId == null) {
            group.setOpinionScope(scope == null ? SCOPE_SUMMARY : scope);
            group.setUpdateBy(operatorId);
            blogGroupService.updateById(group);
            log.info("组长 {} 把小组 {} 的答辩意见可见性改为 {}", operatorId, groupId, group.getOpinionScope());
            return;
        }
        BlogOpinion op = getById(opinionId);
        if (op == null || !op.getGroupId().equals(groupId)) {
            throw new BusinessException("意见不存在或不属于该小组");
        }
        // scope 传 null = 恢复跟随全局默认。
        //
        // ⚠️ 这里不能用 op.setScopeOverride(null) + updateById(op)：
        // MyBatis-Plus 的 updateById 默认字段策略是 NOT_NULL，null 值根本不进 SQL，
        // 结果就是「恢复跟随全局」这个操作静默失效 —— 点一次没反应，还查不出错。
        // 必须用 UpdateWrapper 显式 set，才能真的把这列写成 NULL。
        baseMapper.update(null, new LambdaUpdateWrapper<BlogOpinion>()
                .eq(BlogOpinion::getId, opinionId)
                .set(BlogOpinion::getScopeOverride, scope)
                .set(BlogOpinion::getUpdateBy, operatorId));
        log.info("组长 {} 把意见 {} 的可见性改为 {}", operatorId, opinionId, scope);
    }

    @Override
    public List<Long> listGroupMemberIds(Long groupId) {
        List<BlogGroupMember> rows = groupMemberMapper.selectList(new LambdaQueryWrapper<BlogGroupMember>()
                .eq(BlogGroupMember::getGroupId, groupId)
                .eq(BlogGroupMember::getStatus, 1));
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyList();
        }
        return rows.stream().map(BlogGroupMember::getUserId).distinct().collect(Collectors.toList());
    }

    // ==================== 内部：可见性核心 ====================

    /**
     * 算一条意见最终生效的可见性。
     * 单条覆盖（scopeOverride）优先，没有就跟随小组全局默认。
     */
    private int resolveScope(BlogOpinion op, BlogGroup group) {
        if (op.getScopeOverride() != null) {
            return op.getScopeOverride();
        }
        if (group != null && group.getOpinionScope() != null) {
            return group.getOpinionScope();
        }
        return SCOPE_SUMMARY;
    }

    /**
     * 把数据库记录转成 VO，并按可见性裁剪。组外 + 仅组内的记录直接过滤掉。
     */
    private IPage<OpinionVO> convert(IPage<BlogOpinion> raw, BlogGroup group, boolean insider) {
        List<OpinionVO> out = new ArrayList<>();
        for (BlogOpinion op : raw.getRecords()) {
            int effective = resolveScope(op, group);
            if (!insider && effective == SCOPE_INNER_ONLY) {
                continue;   // 组外人员看不到「仅组内」的记录
            }
            out.add(toVO(op, group, insider, effective));
        }
        Page<OpinionVO> result = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        result.setRecords(out);
        return result;
    }

    private OpinionVO toVO(BlogOpinion op, BlogGroup group, boolean insider, int effective) {
        OpinionVO vo = new OpinionVO();
        vo.setId(op.getId());
        vo.setGroupId(op.getGroupId());
        vo.setGroupName(group == null ? null : group.getName());
        vo.setArticleId(op.getArticleId());
        vo.setTitle(op.getTitle());
        vo.setOpinionType(op.getOpinionType());
        vo.setOpinionTypeName(typeName(op.getOpinionType()));
        vo.setSource(op.getSource());
        vo.setVideoUrl(op.getVideoUrl());
        vo.setCreateTime(op.getCreateTime());
        vo.setUpdateTime(op.getUpdateTime());
        vo.setEffectiveScope(effective);
        vo.setScopeOverride(op.getScopeOverride());
        vo.setCanViewAll(insider || effective == SCOPE_ALL);
        // 上传人ID 带上，昵称由 admin 层回填；前端不显示这个字段
        vo.setUploaderId(op.getUploaderId());

        // ---- 正文：按可见性决定给全量还是给摘要 ----
        String content = op.getContent();
        if (insider || effective == SCOPE_ALL) {
            vo.setContent(content);
            vo.setTruncated(false);
        } else {
            // 可见摘要：截断 + 打标记，前端据此显示「加入小组查看全部」
            vo.setContent(truncate(content, SUMMARY_LENGTH));
            vo.setTruncated(content != null && content.length() > SUMMARY_LENGTH);
        }

        // ---- 附件：摘要模式下不给下载地址，否则截断正文就白做了 ----
        if (insider || effective == SCOPE_ALL) {
            vo.setAttachments(splitFiles(op.getAttachments()));
        } else {
            vo.setAttachments(Collections.emptyList());
        }
        // 上传人昵称由 admin 层回填
        return vo;
    }

    private String truncate(String s, int len) {
        if (s == null) {
            return null;
        }
        return s.length() <= len ? s : s.substring(0, len) + "……";
    }

    private List<String> splitFiles(String s) {
        if (!StringUtils.hasText(s)) {
            return Collections.emptyList();
        }
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private String typeName(Integer type) {
        if (type == null) {
            return "答辩问题";
        }
        return switch (type) {
            case 2 -> "答辩意见";
            case 3 -> "修改建议";
            default -> "答辩问题";
        };
    }

    private long normPage(Long p) {
        return (p == null || p < 1) ? 1 : p;
    }

    private long normSize(Long s) {
        if (s == null || s < 1) {
            return 10;
        }
        return Math.min(s, 100);
    }
}
