package com.qll.ucch.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qll.ucch.mapper.BlogArticleInfoMapper;
import com.qll.ucch.mapper.BlogRegistrationMapper;
import com.qll.ucch.mapper.BlogTypeMapper;
import com.qll.ucch.models.common.BusinessException;
import com.qll.ucch.models.dto.CollabProgressDTO;
import com.qll.ucch.models.dto.RegistrationHandleDTO;
import com.qll.ucch.models.dto.RegistrationQueryDTO;
import com.qll.ucch.models.dto.RegistrationSubmitDTO;
import com.qll.ucch.models.enums.ArticleStatusEnum;
import com.qll.ucch.models.enums.CollabProgressEnum;
import com.qll.ucch.models.enums.NoticeMsgTypeEnum;
import com.qll.ucch.models.po.BlogArticleInfo;
import com.qll.ucch.models.po.BlogRegistration;
import com.qll.ucch.models.po.BlogType;
import com.qll.ucch.models.vo.RegistrationVO;
import com.qll.ucch.service.IBlogRegistrationService;
import com.qll.ucch.models.dto.TmpRoomCreateDTO;
import com.qll.ucch.models.vo.TmpRoomVO;
import com.qll.ucch.service.INoticeService;
import com.qll.ucch.service.ITmpRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 对接申请业务实现。
 *
 * <p>权限判定就一条主线：申请记录里的 user_id 是申请人，文章里的 user_id 是「接单方」，也就是处理人。
 * 谁能看、谁能改都由这两个身份决定。
 *
 * @author qll
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogRegistrationServiceImpl extends ServiceImpl<BlogRegistrationMapper, BlogRegistration> implements IBlogRegistrationService {

    private final BlogRegistrationMapper blogRegistrationMapper;

    private final BlogArticleInfoMapper blogArticleInfoMapper;

    private final BlogTypeMapper blogTypeMapper;

    private final INoticeService noticeService;

    /**
     * 临时沟通房间服务。
     * blog 依赖 notice（单向），所以这里能直接注入 —— 别反过来。
     */
    private final ITmpRoomService tmpRoomService;

    // ==========================================================
    // 提交申请
    // ==========================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitRegistration(RegistrationSubmitDTO dto, Long applicantId, boolean isTeacher) {
        if (applicantId == null) {
            throw new BusinessException("请先登录");
        }
        if (dto == null || dto.getArticleId() == null) {
            throw new BusinessException("请选择要对接的内容");
        }

        BlogArticleInfo article = blogArticleInfoMapper.selectById(dto.getArticleId());
        if (article == null) {
            throw new BusinessException("目标内容不存在或已被删除");
        }
        if (!ArticleStatusEnum.PUBLISHED.getId().equals(article.getStatusId())) {
            throw new BusinessException("该内容尚未发布，暂时无法对接");
        }
        // 自己的内容不用申请对接自己
        if (applicantId.equals(article.getUserId())) {
            throw new BusinessException("不能对自己发布的内容发起对接申请");
        }

        // 校验按角色分两套（校验放 Service 而不是 DTO 上的 @NotNull，是为了能给出具体提示）
        //
        // 学生：必填「年级 + 班级」—— 组长要据此判断这人是不是合适的人
        // 老师：不填年级班级（老师本来就教课的），必填「为什么感兴趣」
        if (isTeacher) {
            if (StrUtil.isBlank(dto.getReason())) {
                throw new BusinessException("请填写你为什么感兴趣");
            }
        } else {
            if (StrUtil.isBlank(dto.getGrade()) || StrUtil.isBlank(dto.getClassName())) {
                throw new BusinessException("请填写你的年级和班级");
            }
        }

        // 同一个人对同一篇文章重复提交没意义，挡一下
        LambdaQueryWrapper<BlogRegistration> dupWrapper = new LambdaQueryWrapper<>();
        dupWrapper.eq(BlogRegistration::getArticleId, dto.getArticleId())
                .eq(BlogRegistration::getUserId, applicantId);
        Long dupCount = blogRegistrationMapper.selectCount(dupWrapper);
        if (dupCount != null && dupCount > 0) {
            throw new BusinessException("你已经提交过对接申请，请等待对方处理");
        }

        BlogRegistration po = new BlogRegistration();
        po.setArticleId(dto.getArticleId());
        // type_id 从文章上带过来，省得前端传错
        po.setTypeId(article.getTypeId());
        po.setUserId(applicantId);
        // 本轮改造：申请内容以「年级 + 班级 + 擅长部分」为主
        po.setGrade(dto.getGrade());
        po.setClassName(dto.getClassName());
        po.setSkills(dto.getSkills());
        // reason 降级为补充说明；联系方式改为选填（可走临时通道沟通）
        po.setReason(dto.getReason());
        po.setContactWay(dto.getContactWay());
        po.setContactWayValue(dto.getContactWayValue());
        po.setAttachments(dto.getAttachments());
        // 刚提交时 pass 留空表示没处理，进度从「待处理」起步
        po.setPass(null);
        po.setCollabProgress(CollabProgressEnum.WAIT.getCode());
        blogRegistrationMapper.insert(po);

        // 通知文章发布者：有人来对接了
        StringBuilder content = new StringBuilder();
        content.append("有人申请对接您发布的《").append(article.getTitle()).append("》");
        if (StrUtil.isNotBlank(dto.getReason())) {
            content.append("，申请理由：").append(dto.getReason());
        }
        sendNoticeQuietly(article.getUserId(), "收到新的对接申请",
                content.toString(), NoticeMsgTypeEnum.RECEIVE_REGISTRATION.getCode());

        log.info("用户 {} 提交对接申请成功，registrationId={}, articleId={}",
                applicantId, po.getId(), dto.getArticleId());
        return po.getId();
    }

    // ==========================================================
    // 列表 / 详情
    // ==========================================================

    @Override
    public Page<RegistrationVO> pageRegistration(RegistrationQueryDTO query, Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException("请先登录");
        }
        RegistrationQueryDTO condition = query == null ? new RegistrationQueryDTO() : query;
        long current = normalizePageNum(condition.getPageNum());
        long size = normalizePageSize(condition.getPageSize());

        boolean received = RegistrationQueryDTO.DIRECTION_RECEIVED.equalsIgnoreCase(condition.getDirection());

        LambdaQueryWrapper<BlogRegistration> wrapper = new LambdaQueryWrapper<>();
        if (received) {
            // 我收到的：先找出「我发布的文章」，再按文章ID反查申请
            List<Long> myArticleIds = listMyArticleIds(currentUserId);
            if (myArticleIds.isEmpty()) {
                // 一篇都没发过，自然没有收到的申请，直接返回空页
                return new Page<>(current, size, 0);
            }
            wrapper.in(BlogRegistration::getArticleId, myArticleIds);
        } else {
            // 我发出的：直接按申请人筛
            wrapper.eq(BlogRegistration::getUserId, currentUserId);
        }

        wrapper.eq(condition.getArticleId() != null, BlogRegistration::getArticleId, condition.getArticleId())
                .eq(condition.getTypeId() != null, BlogRegistration::getTypeId, condition.getTypeId())
                .eq(condition.getPass() != null, BlogRegistration::getPass, condition.getPass())
                .eq(condition.getCollabProgress() != null,
                        BlogRegistration::getCollabProgress, condition.getCollabProgress())
                .orderByDesc(BlogRegistration::getCreateTime);

        Page<BlogRegistration> poPage = blogRegistrationMapper.selectPage(new Page<>(current, size), wrapper);
        List<RegistrationVO> records = assembleVOList(poPage.getRecords(), !received);

        Page<RegistrationVO> voPage = new Page<>(current, size, poPage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public RegistrationVO getRegistrationDetail(Long id, Long currentUserId) {
        if (id == null) {
            throw new BusinessException("申请ID不能为空");
        }
        BlogRegistration po = blogRegistrationMapper.selectById(id);
        if (po == null) {
            throw new BusinessException("对接申请不存在或已被删除");
        }
        if (currentUserId == null) {
            throw new BusinessException("请先登录");
        }

        BlogArticleInfo article = blogArticleInfoMapper.selectById(po.getArticleId());
        Long articleOwnerId = article == null ? null : article.getUserId();
        // 只有申请人和文章发布者两个人能看这条记录
        boolean isApplicant = currentUserId.equals(po.getUserId());
        boolean isOwner = currentUserId.equals(articleOwnerId);
        if (!isApplicant && !isOwner) {
            throw new BusinessException("没有权限查看该对接申请");
        }

        RegistrationVO vo = toVO(po, article, null);
        vo.setArticleOwnerId(articleOwnerId);
        return vo;
    }

    // ==========================================================
    // 处理 / 进度
    // ==========================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRegistration(RegistrationHandleDTO dto, Long handlerId) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("申请ID不能为空");
        }
        if (dto.getPass() == null) {
            throw new BusinessException("请选择处理结果");
        }
        if (handlerId == null) {
            throw new BusinessException("请先登录");
        }

        BlogRegistration po = blogRegistrationMapper.selectById(dto.getId());
        if (po == null) {
            throw new BusinessException("对接申请不存在或已被删除");
        }
        BlogArticleInfo article = blogArticleInfoMapper.selectById(po.getArticleId());
        if (article == null) {
            throw new BusinessException("关联的内容不存在或已被删除");
        }
        // 只有文章发布者能处理别人发来的申请
        if (!handlerId.equals(article.getUserId())) {
            throw new BusinessException("只有内容发布者才能处理该申请");
        }
        if (po.getPass() != null) {
            throw new BusinessException("该申请已经处理过了");
        }

        boolean pass = dto.getPass() == 1;
        Integer progress = pass ? CollabProgressEnum.PASSED.getCode() : CollabProgressEnum.REFUSED.getCode();

        LambdaUpdateWrapper<BlogRegistration> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BlogRegistration::getId, po.getId())
                .set(BlogRegistration::getPass, dto.getPass())
                .set(BlogRegistration::getReviewMessage, dto.getReviewMessage())
                .set(BlogRegistration::getCollabProgress, progress)
                .set(BlogRegistration::getUpdateBy, handlerId);
        blogRegistrationMapper.update(null, updateWrapper);

        // 结果告诉申请人，附上对方的联系方式（通过的话才给看）
        StringBuilder content = new StringBuilder();
        content.append("您对《").append(article.getTitle()).append("》的对接申请");
        if (pass) {
            content.append("已通过。");
            if (StrUtil.isNotBlank(po.getContactWay()) || StrUtil.isNotBlank(po.getContactWayValue())) {
                content.append("对方联系方式：")
                        .append(StrUtil.blankToDefault(po.getContactWay(), "未填写"))
                        .append(" ")
                        .append(StrUtil.blankToDefault(po.getContactWayValue(), ""));
            }
        } else {
            content.append("被拒绝了。");
        }
        if (StrUtil.isNotBlank(dto.getReviewMessage())) {
            content.append("回复：").append(dto.getReviewMessage());
        }
        // 通过的话，给双方开一个临时沟通通道
        String roomHint = "";
        if (pass) {
            roomHint = openTmpRoomQuietly(po, article, handlerId);
            if (StrUtil.isNotBlank(roomHint)) {
                content.append(roomHint);
            }
        }

        sendNoticeQuietly(po.getUserId(), pass ? "对接申请已通过" : "对接申请被拒绝",
                content.toString(), NoticeMsgTypeEnum.RECEIVE_REGISTRATION.getCode());

        log.info("对接申请 {} 处理完成，pass={}, handlerId={}", po.getId(), dto.getPass(), handlerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCollabProgress(CollabProgressDTO dto, Long operatorId) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("申请ID不能为空");
        }
        if (dto.getCollabProgress() == null) {
            throw new BusinessException("请选择对接进度");
        }
        if (operatorId == null) {
            throw new BusinessException("请先登录");
        }

        CollabProgressEnum target = CollabProgressEnum.of(dto.getCollabProgress());
        if (target == null) {
            throw new BusinessException("对接进度不合法");
        }

        BlogRegistration po = blogRegistrationMapper.selectById(dto.getId());
        if (po == null) {
            throw new BusinessException("对接申请不存在或已被删除");
        }
        BlogArticleInfo article = blogArticleInfoMapper.selectById(po.getArticleId());
        Long articleOwnerId = article == null ? null : article.getUserId();

        // 对接双方都能推进度，但别的人不行
        boolean isApplicant = operatorId.equals(po.getUserId());
        boolean isOwner = operatorId.equals(articleOwnerId);
        if (!isApplicant && !isOwner) {
            throw new BusinessException("没有权限更新该对接进度");
        }

        // 拒绝之前必须先把申请处理掉，否则进度和 pass 会对不上
        int currentCode = po.getCollabProgress() == null
                ? CollabProgressEnum.WAIT.getCode() : po.getCollabProgress();
        CollabProgressEnum currentEnum = CollabProgressEnum.of(currentCode);
        if (!currentEnum.canTransferTo(target.getCode())) {
            throw new BusinessException("当前进度「" + currentEnum.getName()
                    + "」不能直接改为「" + target.getName() + "」");
        }

        // 往拒绝方向走的时候，把 pass 一并置为 0，保持两个字段语义一致
        Integer pass = po.getPass();
        if (target == CollabProgressEnum.REFUSED) {
            pass = 0;
        } else if (target == CollabProgressEnum.PASSED) {
            pass = 1;
        }

        String reviewMessage = po.getReviewMessage();
        if (StrUtil.isNotBlank(dto.getRemark())) {
            // 备注追加在原有回复后面，保留历史沟通内容
            reviewMessage = StrUtil.isBlank(reviewMessage)
                    ? dto.getRemark()
                    : reviewMessage + " | " + dto.getRemark();
        }

        LambdaUpdateWrapper<BlogRegistration> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BlogRegistration::getId, po.getId())
                .set(BlogRegistration::getCollabProgress, target.getCode())
                .set(BlogRegistration::getPass, pass)
                .set(BlogRegistration::getReviewMessage, reviewMessage)
                .set(BlogRegistration::getUpdateBy, operatorId);
        blogRegistrationMapper.update(null, updateWrapper);

        // 通知对接的另一方进度变了
        Long notifyUserId = isApplicant ? articleOwnerId : po.getUserId();
        StringBuilder content = new StringBuilder();
        content.append("《").append(article == null ? "您关注的内容" : article.getTitle()).append("》");
        content.append("的对接进度更新为：").append(target.getName());
        if (StrUtil.isNotBlank(dto.getRemark())) {
            content.append("，说明：").append(dto.getRemark());
        }
        sendNoticeQuietly(notifyUserId, "对接进度更新", content.toString(),
                NoticeMsgTypeEnum.RECEIVE_REGISTRATION.getCode());

        log.info("对接申请 {} 进度更新：{} -> {}，操作人={}",
                po.getId(), currentEnum.getName(), target.getName(), operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelRegistration(Long id, Long applicantId) {
        if (id == null) {
            throw new BusinessException("申请ID不能为空");
        }
        BlogRegistration po = blogRegistrationMapper.selectById(id);
        if (po == null) {
            throw new BusinessException("对接申请不存在或已被删除");
        }
        if (applicantId == null || !applicantId.equals(po.getUserId())) {
            throw new BusinessException("只能撤回自己提交的申请");
        }
        // 逻辑删除，记录留着方便追溯
        blogRegistrationMapper.deleteById(id);
        log.info("用户 {} 撤回对接申请 {}", applicantId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRegistration(Long id, Long operatorId) {
        if (id == null) {
            throw new BusinessException("申请ID不能为空");
        }
        BlogRegistration po = blogRegistrationMapper.selectById(id);
        if (po == null) {
            throw new BusinessException("对接申请不存在或已被删除");
        }
        if (operatorId == null) {
            throw new BusinessException("请先登录");
        }
        BlogArticleInfo article = blogArticleInfoMapper.selectById(po.getArticleId());
        Long articleOwnerId = article == null ? null : article.getUserId();

        // 申请人和发布者都能删掉自己这边的记录
        if (!operatorId.equals(po.getUserId()) && !operatorId.equals(articleOwnerId)) {
            throw new BusinessException("没有权限删除该对接申请");
        }
        blogRegistrationMapper.deleteById(id);
        log.info("用户 {} 删除对接申请 {}", operatorId, id);
    }

    @Override
    public Long countByArticle(Long articleId) {
        if (articleId == null) {
            return 0L;
        }
        LambdaQueryWrapper<BlogRegistration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogRegistration::getArticleId, articleId);
        return blogRegistrationMapper.selectCount(wrapper);
    }

    @Override
    public Long countPendingReceived(Long ownerId) {
        if (ownerId == null) {
            return 0L;
        }
        List<Long> myArticleIds = listMyArticleIds(ownerId);
        if (myArticleIds.isEmpty()) {
            return 0L;
        }
        LambdaQueryWrapper<BlogRegistration> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(BlogRegistration::getArticleId, myArticleIds)
                // 还没处理过的才算待办
                .isNull(BlogRegistration::getPass);
        return blogRegistrationMapper.selectCount(wrapper);
    }

    // ==========================================================
    // 内部工具方法
    // ==========================================================

    /**
     * 查某个用户发布的全部文章ID（逻辑删除的会自动排除）。
     */
    private List<Long> listMyArticleIds(Long userId) {
        LambdaQueryWrapper<BlogArticleInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogArticleInfo::getUserId, userId)
                .select(BlogArticleInfo::getId);
        return blogArticleInfoMapper.selectList(wrapper).stream()
                .map(BlogArticleInfo::getId)
                .collect(Collectors.toList());
    }

    /**
     * 批量转 VO。文章和类型都一次性查出来做映射，避免 N+1。
     *
     * @param sent true=我发出的列表（展示文章信息）；false=我收到的列表（展示申请人信息）
     */
    private List<RegistrationVO> assembleVOList(List<BlogRegistration> list, boolean sent) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> articleIds = list.stream()
                .map(BlogRegistration::getArticleId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, BlogArticleInfo> articleMap = new HashMap<>(articleIds.size());
        if (!articleIds.isEmpty()) {
            List<BlogArticleInfo> articles = blogArticleInfoMapper.selectBatchIds(articleIds);
            for (BlogArticleInfo article : articles) {
                articleMap.put(article.getId(), article);
            }
        }

        Map<Long, String> typeMap = new HashMap<>();
        List<BlogType> types = blogTypeMapper.selectList(new LambdaQueryWrapper<>());
        for (BlogType type : types) {
            typeMap.put(type.getId(), type.getName());
        }

        List<RegistrationVO> result = new ArrayList<>(list.size());
        for (BlogRegistration po : list) {
            result.add(toVO(po, articleMap.get(po.getArticleId()), typeMap));
        }
        return result;
    }

    /**
     * PO -> VO。申请人的昵称本模块拿不到（在 sys 模块），先留空由上层补。
     */
    private RegistrationVO toVO(BlogRegistration po, BlogArticleInfo article, Map<Long, String> typeMap) {
        RegistrationVO vo = new RegistrationVO();
        vo.setId(po.getId());
        vo.setArticleId(po.getArticleId());
        vo.setTypeId(po.getTypeId());
        vo.setUserId(po.getUserId());
        // 本轮改造：年级 / 班级 / 擅长 要回给前端 ——
        // 组长审核申请时就是靠这三样判断「这人合不合适」的。
        // 数据库里已经存了，但这里不 set 的话接口就是不给（本次实测踩过这个坑）。
        vo.setGrade(po.getGrade());
        vo.setClassName(po.getClassName());
        vo.setSkills(po.getSkills());
        // 顺便拆成数组，前端直接 v-for 渲染标签，不用自己 split
        if (StrUtil.isNotBlank(po.getSkills())) {
            vo.setSkillList(java.util.Arrays.stream(po.getSkills().split(","))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toList()));
        } else {
            vo.setSkillList(java.util.Collections.emptyList());
        }
        vo.setReason(po.getReason());
        vo.setContactWay(po.getContactWay());
        vo.setContactWayValue(po.getContactWayValue());
        vo.setAttachments(po.getAttachments());
        vo.setPass(po.getPass());
        vo.setReviewMessage(po.getReviewMessage());
        vo.setCollabProgress(po.getCollabProgress());
        vo.setCreateTime(po.getCreateTime());
        vo.setUpdateTime(po.getUpdateTime());

        CollabProgressEnum progressEnum = CollabProgressEnum.of(po.getCollabProgress());
        vo.setCollabProgressName(progressEnum == null ? null : progressEnum.getName());

        if (typeMap != null) {
            vo.setTypeName(typeMap.get(po.getTypeId()));
        }
        if (article != null) {
            vo.setArticleTitle(article.getTitle());
            vo.setArticleOwnerId(article.getUserId());
        }
        return vo;
    }

    /**
     * 通知失败不影响主流程，包装一下吞掉异常。
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
        return Math.min(pageSize, 100L);
    }

    /**
     * 申请通过后开一个临时沟通通道。
     *
     * 照 sendNoticeQuietly 的思路：**开通道失败只记日志，不能让它拖垮审核主流程** ——
     * 审核通过这件事本身已经落库了，通道开不出来顶多是双方先互换联系方式，
     * 不能因为通道的问题让「通过」这个操作失败。
     *
     * @return 给申请人看的提示文案（拼进通知里）；开失败返回空串
     */
    private String openTmpRoomQuietly(BlogRegistration po, BlogArticleInfo article, Long handlerId) {
        try {
            TmpRoomCreateDTO dto = new TmpRoomCreateDTO();
            dto.setRegistrationId(po.getId());
            dto.setArticleId(po.getArticleId());
            // 参与人：申请方 + 内容发布方（组长）
            dto.setUserA(po.getUserId());
            dto.setUserB(article.getUserId());
            dto.setTitle(article.getTitle());
            TmpRoomVO room = tmpRoomService.createRoom(handlerId, dto);
            log.info("申请 {} 通过，已开临时通道 {}", po.getId(), room.getId());
            return "已为你和对方开通临时沟通通道（有效期 " + room.getRemainingHours()
                    + " 小时），可在「我的申请」里进入。";
        } catch (Exception e) {
            log.warn("开临时沟通通道失败（不影响审核结果）：{}", e.getMessage());
            return "";
        }
    }
}
