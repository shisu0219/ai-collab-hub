package com.qll.ucch.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qll.ucch.mapper.ChatMessageMapper;
import com.qll.ucch.mapper.NoticeBoxInfoMapper;
import com.qll.ucch.models.dto.NoticeQueryDTO;
import com.qll.ucch.models.dto.NoticeSendDTO;
import com.qll.ucch.models.po.ChatMessage;
import com.qll.ucch.models.po.NoticeBoxInfo;
import com.qll.ucch.models.vo.NoticeUnreadVO;
import com.qll.ucch.models.vo.NoticeVO;
import com.qll.ucch.service.INoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import com.qll.ucch.exception.BusinessException;

/**
 * 消息通知服务实现。
 * <p>
 * 表是 notice_box_info（原来的 noice_box_info 拼错了，建表脚本已修正，
 * 实体上的 @TableName 也跟着改了）。
 *
 * @author qll
 */
@Slf4j
@Service
public class NoticeServiceImpl implements INoticeService {

    /** 消息分类：审核结果 */
    public static final int MSG_TYPE_AUDIT = 1;
    /** 消息分类：收到申请 */
    public static final int MSG_TYPE_APPLY = 2;
    /** 消息分类：系统通知 */
    public static final int MSG_TYPE_SYSTEM = 3;
    /** 消息分类：会话消息 */
    public static final int MSG_TYPE_CHAT = 4;

    /** 未读 */
    private static final int UNREAD = 0;
    /** 已读 */
    private static final int READ = 1;

    private final NoticeBoxInfoMapper noticeBoxInfoMapper;

    /**
     * 聊天消息 Mapper。
     * <p>
     * 【为什么不注入 IChatService】
     * ChatServiceImpl 已经注入了 INoticeService（发消息要发通知），
     * 这里再注入 IChatService 就形成双向依赖，Spring 3.x 默认禁止循环引用，启动会失败。
     * 两个 Mapper 都在 notice 模块内，直接用 Mapper 更新，绕开 Service 层也就没有循环。
     */
    private final ChatMessageMapper chatMessageMapper;

    @Autowired
    public NoticeServiceImpl(NoticeBoxInfoMapper noticeBoxInfoMapper,
                             ChatMessageMapper chatMessageMapper) {
        this.noticeBoxInfoMapper = noticeBoxInfoMapper;
        this.chatMessageMapper = chatMessageMapper;
    }

    // ==================== 发送 ====================

    /**
     * 发送站内消息。签名是给 blog 模块用的，不要改参数顺序。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendNotice(Long userId, String title, String content, Integer msgType) {
        if (userId == null) {
            // 没有接收人就没什么可发的，静默返回，避免影响调用方主流程
            return;
        }
        NoticeBoxInfo notice = new NoticeBoxInfo();
        notice.setUserId(userId);
        notice.setTitle(StrUtil.blankToDefault(title, "系统通知"));
        notice.setContent(content);
        notice.setMsgType(msgType == null ? MSG_TYPE_SYSTEM : msgType);
        notice.setReadStatus(UNREAD);
        noticeBoxInfoMapper.insert(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendNotice(Long userId, String title, String content, Integer msgType, Long refId) {
        if (userId == null) {
            // 没有接收人就没什么可发的，静默返回，避免影响调用方主流程
            return;
        }
        NoticeBoxInfo notice = new NoticeBoxInfo();
        notice.setUserId(userId);
        notice.setTitle(StrUtil.blankToDefault(title, "系统通知"));
        notice.setContent(content);
        notice.setMsgType(msgType == null ? MSG_TYPE_SYSTEM : msgType);
        notice.setReadStatus(UNREAD);
        // 关联对象ID：会话消息存会话ID，用于两个红点联动已读
        notice.setRefId(refId);
        noticeBoxInfoMapper.insert(notice);
    }

    /**
     * 按关联对象标记已读（红点联动的核心）。
     *
     * 只清 ref_id 匹配的那些通知 —— 这样读完 A 会话不会误清 B 会话的通知。
     * userId 一定要带上，防止越权标别人的消息。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markReadByRef(Long userId, Long refId) {
        if (userId == null || refId == null) {
            return 0;
        }
        LambdaUpdateWrapper<NoticeBoxInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(NoticeBoxInfo::getUserId, userId)
                .eq(NoticeBoxInfo::getRefId, refId)
                .eq(NoticeBoxInfo::getReadStatus, UNREAD)
                .set(NoticeBoxInfo::getReadStatus, READ);
        return noticeBoxInfoMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendNotice(NoticeSendDTO dto) {
        if (dto == null) {
            return;
        }
        sendNotice(dto.getUserId(), dto.getTitle(), dto.getContent(), dto.getMsgType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendNoticeBatch(List<Long> userIds, String title, String content, Integer msgType) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (Long userId : userIds) {
            sendNotice(userId, title, content, msgType);
        }
    }

    // ==================== 查询 ====================

    @Override
    public IPage<NoticeVO> pageMyNotice(Long userId, NoticeQueryDTO query) {
        NoticeQueryDTO condition = query == null ? new NoticeQueryDTO() : query;
        long pageNum = condition.getPageNum() == null || condition.getPageNum() < 1 ? 1 : condition.getPageNum();
        long pageSize = condition.getPageSize() == null || condition.getPageSize() < 1 ? 10 : condition.getPageSize();
        // 防止前端传个 10000 把库拖死
        if (pageSize > 100) {
            pageSize = 100;
        }

        LambdaQueryWrapper<NoticeBoxInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoticeBoxInfo::getUserId, userId);
        // 分类过滤：这是新增功能，以前只能一锅看
        wrapper.eq(condition.getMsgType() != null, NoticeBoxInfo::getMsgType, condition.getMsgType());
        // 标题模糊搜索
        wrapper.like(StrUtil.isNotBlank(condition.getTitle()), NoticeBoxInfo::getTitle, condition.getTitle());
        wrapper.eq(condition.getReadStatus() != null, NoticeBoxInfo::getReadStatus, condition.getReadStatus());
        wrapper.orderByDesc(NoticeBoxInfo::getCreateTime);

        Page<NoticeBoxInfo> page = noticeBoxInfoMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        // PO 转 VO，顺便把分类中文名和格式化时间补上
        Page<NoticeVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<NoticeVO> records = new ArrayList<>();
        for (NoticeBoxInfo po : page.getRecords()) {
            records.add(toVo(po));
        }
        result.setRecords(records);
        return result;
    }

    @Override
    public List<NoticeVO> listUnread(Long userId) {
        LambdaQueryWrapper<NoticeBoxInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoticeBoxInfo::getUserId, userId)
                .eq(NoticeBoxInfo::getReadStatus, UNREAD)
                .orderByDesc(NoticeBoxInfo::getCreateTime)
                .last("LIMIT 100");
        List<NoticeBoxInfo> list = noticeBoxInfoMapper.selectList(wrapper);
        List<NoticeVO> result = new ArrayList<>(list.size());
        for (NoticeBoxInfo po : list) {
            result.add(toVo(po));
        }
        return result;
    }

    @Override
    public NoticeVO getDetail(Long userId, Long noticeId) {
        NoticeBoxInfo po = getOwnNotice(userId, noticeId);
        return toVo(po);
    }

    // ==================== 状态变更 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markRead(Long userId, Long noticeId) {
        // 先确认这条消息确实是他的，防止越权点别人的消息
        NoticeBoxInfo po = getOwnNotice(userId, noticeId);
        if (po.getReadStatus() != null && po.getReadStatus() == READ) {
            // 已经读过了，不用再更新
            return true;
        }
        LambdaUpdateWrapper<NoticeBoxInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(NoticeBoxInfo::getId, noticeId)
                .eq(NoticeBoxInfo::getUserId, userId)
                .set(NoticeBoxInfo::getReadStatus, READ);
        return noticeBoxInfoMapper.update(null, wrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markAllRead(Long userId, Integer msgType) {
        LambdaUpdateWrapper<NoticeBoxInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(NoticeBoxInfo::getUserId, userId)
                .eq(NoticeBoxInfo::getReadStatus, UNREAD)
                .eq(msgType != null, NoticeBoxInfo::getMsgType, msgType)
                .set(NoticeBoxInfo::getReadStatus, READ);
        int changed = noticeBoxInfoMapper.update(null, wrapper);

        /*
         * 【红点联动 - 反方向】把聊天消息的未读也清掉。
         *
         * 场景：用户在消息中心点「全部已读」，但顶部「站内沟通」的红点还在，
         * 因为那个红点数的是 chat_message.read_status。
         *
         * 【为什么这里直接注入 ChatMessageMapper 而不是 IChatService】
         * ChatServiceImpl 已经注入了 INoticeService（发消息时要发通知）。
         * 如果这里再注入 IChatService，就形成双向依赖，Spring 3.x 默认禁止循环引用，启动会失败。
         * 两个 Mapper 都在 notice 模块内，直接用 Mapper 更新，不绕道 Service，也就没有循环。
         *
         * 只在「不按分类」或「按会话消息分类」时才动 chat_message ——
         * 用户如果只想清「审核结果」那一类，不该把聊天未读也清掉。
         */
        boolean shouldClearChat = (msgType == null) || (msgType == MSG_TYPE_CHAT);
        if (shouldClearChat) {
            try {
                LambdaUpdateWrapper<ChatMessage> chatWrapper = new LambdaUpdateWrapper<>();
                chatWrapper
                        // 只清「别人发给我的」，自己发的不算未读
                        .ne(ChatMessage::getSenderId, userId)
                        .eq(ChatMessage::getReadStatus, UNREAD)
                        // 限定在「我参与的会话」里
                        .inSql(ChatMessage::getSessionId,
                                "SELECT id FROM chat_session WHERE user_a = " + userId
                                        + " OR user_b = " + userId)
                        .set(ChatMessage::getReadStatus, READ);
                chatMessageMapper.update(null, chatWrapper);
            } catch (Exception e) {
                log.warn("联动清聊天未读失败，userId={}, 原因：{}", userId, e.getMessage());
            }
        }

        return changed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNotice(Long userId, Long noticeId) {
        // 逻辑删除，MyBatis-Plus 的 @TableLogic 会自动把 delete 变成 update delete_time
        NoticeBoxInfo po = getOwnNotice(userId, noticeId);
        return noticeBoxInfoMapper.deleteById(po.getId()) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBatch(Long userId, List<Long> noticeIds) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            return 0;
        }
        LambdaQueryWrapper<NoticeBoxInfo> wrapper = new LambdaQueryWrapper<>();
        // 带 userId 条件，防止误删别人的消息
        wrapper.eq(NoticeBoxInfo::getUserId, userId)
                .in(NoticeBoxInfo::getId, noticeIds);
        return noticeBoxInfoMapper.delete(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int clearAll(Long userId, Integer msgType) {
        LambdaQueryWrapper<NoticeBoxInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoticeBoxInfo::getUserId, userId)
                .eq(msgType != null, NoticeBoxInfo::getMsgType, msgType);
        return noticeBoxInfoMapper.delete(wrapper);
    }

    // ==================== 统计 ====================

    @Override
    public NoticeUnreadVO countUnread(Long userId) {
        NoticeUnreadVO vo = new NoticeUnreadVO();
        if (userId == null) {
            return vo;
        }
        vo.setAuditCount(countUnreadByType(userId, MSG_TYPE_AUDIT));
        vo.setApplyCount(countUnreadByType(userId, MSG_TYPE_APPLY));
        vo.setSystemCount(countUnreadByType(userId, MSG_TYPE_SYSTEM));
        vo.setChatCount(countUnreadByType(userId, MSG_TYPE_CHAT));
        vo.setTotal(countUnreadByType(userId, null));
        return vo;
    }

    @Override
    public long countUnreadByType(Long userId, Integer msgType) {
        if (userId == null) {
            return 0L;
        }
        LambdaQueryWrapper<NoticeBoxInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoticeBoxInfo::getUserId, userId)
                .eq(NoticeBoxInfo::getReadStatus, UNREAD)
                .eq(msgType != null, NoticeBoxInfo::getMsgType, msgType);
        Long count = noticeBoxInfoMapper.selectCount(wrapper);
        return count == null ? 0L : count;
    }

    // ==================== 内部方法 ====================

    /**
     * 取一条「属于我的」消息，取不到就抛异常。
     * 消息详情、标记已读、删除都要先过这一关。
     */
    private NoticeBoxInfo getOwnNotice(Long userId, Long noticeId) {
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        if (noticeId == null) {
            throw new BusinessException("消息ID不能为空");
        }
        NoticeBoxInfo po = noticeBoxInfoMapper.selectById(noticeId);
        if (po == null) {
            throw new BusinessException("消息不存在或已删除");
        }
        if (!userId.equals(po.getUserId())) {
            throw new BusinessException("只能操作自己的消息");
        }
        return po;
    }

    /**
     * PO -> VO，补上分类中文和格式化时间
     */
    private NoticeVO toVo(NoticeBoxInfo po) {
        NoticeVO vo = NoticeVO.from(po);
        if (vo != null && vo.getCreateTime() != null) {
            vo.setCreateTimeText(DateUtil.formatDateTime(DateUtil.date(vo.getCreateTime())));
        }
        return vo;
    }
}
