package com.qll.ucch.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qll.ucch.mapper.ChatMessageMapper;
import com.qll.ucch.mapper.ChatSessionMapper;
import com.qll.ucch.models.dto.ChatMessageSendDTO;
import com.qll.ucch.models.dto.ChatSessionCreateDTO;
import com.qll.ucch.models.po.ChatMessage;
import com.qll.ucch.models.po.ChatSession;
import com.qll.ucch.models.vo.ChatMessageVO;
import com.qll.ucch.models.vo.ChatSessionVO;
import com.qll.ucch.service.IChatService;
import com.qll.ucch.service.INoticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.qll.ucch.exception.BusinessException;

/**
 * 站内会话服务实现（新增功能）。
 * <p>
 * 设计上尽量简单：不做 WebSocket，就是普通的「发消息 -> 落库 -> 对方下次拉取时看到」，
 * 对毕设场景够用了。真要实时推送后面再接。
 *
 * @author qll
 */
@Slf4j
@Service
public class ChatServiceImpl implements IChatService {

    /** 消息类型：文本 */
    private static final int KIND_TEXT = 1;
    /** 消息类型：文件 */
    private static final int KIND_FILE = 2;
    /** 消息类型：系统提示 */
    private static final int KIND_SYSTEM = 3;

    /** 未读 */
    private static final int UNREAD = 0;
    /** 已读 */
    private static final int READ = 1;

    /** 会话列表里最后一条消息最多截多长 */
    private static final int PREVIEW_LENGTH = 50;

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final INoticeService noticeService;

    @Autowired
    public ChatServiceImpl(ChatSessionMapper chatSessionMapper,
                           ChatMessageMapper chatMessageMapper,
                           INoticeService noticeService) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.noticeService = noticeService;
    }

    // ==================== 会话 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSession(Long registrationId, Long userA, Long userB, Long articleId) {
        if (registrationId == null) {
            throw new BusinessException("对接申请ID不能为空");
        }
        if (userA == null || userB == null) {
            throw new BusinessException("会话双方不能为空");
        }
        if (userA.equals(userB)) {
            throw new BusinessException("不能和自己创建会话");
        }

        // 一个对接申请只开一个会话，已经有的直接返回，保证幂等
        ChatSession exist = getByRegistrationIdInternal(registrationId);
        if (exist != null) {
            // 文章ID可能之前没传，这次补上
            if (exist.getArticleId() == null && articleId != null) {
                exist.setArticleId(articleId);
                chatSessionMapper.updateById(exist);
            }
            return exist.getId();
        }

        ChatSession session = new ChatSession();
        session.setRegistrationId(registrationId);
        session.setUserA(userA);
        session.setUserB(userB);
        session.setArticleId(articleId);
        session.setLastMsgTime(LocalDateTime.now());
        chatSessionMapper.insert(session);

        // 开个会话给双方各留个痕迹，消息中心能看到
        // 带上 sessionId 作为 refId：这样读完这个会话，消息中心里这两条通知会自动标已读
        noticeService.sendNotice(userA, "已建立沟通会话",
                "对接申请已通过，可以在「我的消息」里和对方沟通了。", 4, session.getId());
        noticeService.sendNotice(userB, "已建立沟通会话",
                "对接申请已通过，可以在「我的消息」里和对方沟通了。", 4, session.getId());

        return session.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSession(ChatSessionCreateDTO dto) {
        if (dto == null) {
            throw new BusinessException("参数不能为空");
        }
        return createSession(dto.getRegistrationId(), dto.getUserA(), dto.getUserB(), dto.getArticleId());
    }

    @Override
    public List<ChatSessionVO> listMySession(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        // 要么是 A 方要么是 B 方，注意括号不能少，不然 or 会把前面的条件带跑偏
        wrapper.and(w -> w.eq(ChatSession::getUserA, userId).or().eq(ChatSession::getUserB, userId));
        wrapper.orderByDesc(ChatSession::getLastMsgTime);
        List<ChatSession> sessions = chatSessionMapper.selectList(wrapper);

        List<ChatSessionVO> result = new ArrayList<>(sessions.size());
        for (ChatSession session : sessions) {
            ChatSessionVO vo = ChatSessionVO.from(session, userId);
            // 补最后一条消息预览 + 我的未读数
            ChatMessage last = getLastMessage(session.getId());
            if (last != null) {
                vo.setLastMessage(StrUtil.maxLength(
                        StrUtil.blankToDefault(last.getContent(), "[文件]"), PREVIEW_LENGTH));
                if (last.getCreateTime() != null) {
                    vo.setLastMsgTime(last.getCreateTime());
                }
            } else {
                vo.setLastMessage("还没有聊过，打个招呼吧");
            }
            vo.setUnreadCount(countUnreadInSession(userId, session.getId()));
            result.add(vo);
        }
        return result;
    }

    @Override
    public ChatSessionVO getByRegistrationId(Long registrationId, Long currentUserId) {
        ChatSession session = getByRegistrationIdInternal(registrationId);
        return ChatSessionVO.from(session, currentUserId);
    }

    @Override
    public ChatSessionVO getSessionDetail(Long userId, Long sessionId) {
        ChatSession session = getOwnSession(userId, sessionId);
        return ChatSessionVO.from(session, userId);
    }

    // ==================== 消息 ====================

    @Override
    public IPage<ChatMessageVO> pageMessage(Long userId, Long sessionId, Integer pageNum, Integer pageSize) {
        // 先卡权限，不是会话参与人直接拒绝
        getOwnSession(userId, sessionId);

        long num = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long size = pageSize == null || pageSize < 1 ? 20 : pageSize;
        if (size > 200) {
            size = 200;
        }

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .orderByDesc(ChatMessage::getCreateTime)
                .orderByDesc(ChatMessage::getId);

        Page<ChatMessage> page = chatMessageMapper.selectPage(new Page<>(num, size), wrapper);
        Page<ChatMessageVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<ChatMessageVO> records = new ArrayList<>(page.getRecords().size());
        for (ChatMessage po : page.getRecords()) {
            records.add(toVo(po, userId));
        }
        result.setRecords(records);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO sendMessage(Long userId, ChatMessageSendDTO dto) {
        if (dto == null) {
            throw new BusinessException("消息内容不能为空");
        }
        // 内容校验：文本要有字，文件要有附件
        dto.validateContent();

        ChatSession session = getOwnSession(userId, dto.getSessionId());
        Long targetUserId = session.getOtherUserId(userId);

        ChatMessage message = new ChatMessage();
        message.setSessionId(session.getId());
        message.setSenderId(userId);
        message.setContent(StrUtil.blankToDefault(dto.getContent(), null));
        message.setMsgKind(dto.getMsgKind() == null ? KIND_TEXT : dto.getMsgKind());
        message.setAttachments(dto.getAttachments());
        message.setReadStatus(UNREAD);
        message.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(message);

        // 会话的最后消息时间要刷新，会话列表按它排序
        LambdaUpdateWrapper<ChatSession> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatSession::getId, session.getId())
                .set(ChatSession::getLastMsgTime, message.getCreateTime());
        chatSessionMapper.update(null, wrapper);

        // 顺带给对方发条站内通知，消息中心的小红点就是这个
        // refId 传会话ID：对方读完这个会话时，这条通知会跟着自动标已读（两个红点联动）
        if (targetUserId != null) {
            String preview = message.getMsgKind() == KIND_FILE
                    ? "对方给你发了一个文件"
                    : StrUtil.maxLength(StrUtil.blankToDefault(message.getContent(), ""), PREVIEW_LENGTH);
            noticeService.sendNotice(targetUserId, "收到一条新消息", preview, 4, session.getId());
        }

        return toVo(message, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markSessionRead(Long userId, Long sessionId) {
        // 同样是先校验是不是自己参与的会话
        getOwnSession(userId, sessionId);

        LambdaUpdateWrapper<ChatMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                // 只把「别人发给我」的标成已读，自己发的不算
                .ne(ChatMessage::getSenderId, userId)
                .eq(ChatMessage::getReadStatus, UNREAD)
                .set(ChatMessage::getReadStatus, READ);
        int changed = chatMessageMapper.update(null, wrapper);

        /*
         * 【红点联动】把消息中心里关于这个会话的通知也标已读。
         *
         * 不加这一步的话会出现：聊天页消息读完了，但顶部「消息中心」的红点还在 ——
         * 因为那两个红点数的是两张表：
         *   站内沟通红点 -> chat_message.read_status
         *   消息中心红点 -> notice_box_info.read_status
         * 读完聊天只动了前一张表，通知表那条「收到一条新消息」还挂着未读。
         *
         * 靠 refId 精确匹配，只清这个会话的通知，别的会话不受影响。
         * 失败不影响主流程（已读已经生效了，通知没清顶多红点多留一会儿）。
         */
        try {
            noticeService.markReadByRef(userId, sessionId);
        } catch (Exception e) {
            log.warn("联动标记消息中心已读失败，userId={}, sessionId={}, 原因：{}",
                    userId, sessionId, e.getMessage());
        }

        return changed;
    }

    @Override
    public long countSessionUnread(Long userId, Long sessionId) {
        return countUnreadInSession(userId, sessionId);
    }

    @Override
    public long countAllSessionUnread(Long userId) {
        if (userId == null) {
            return 0L;
        }
        List<ChatSession> sessions = chatSessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .and(w -> w.eq(ChatSession::getUserA, userId).or().eq(ChatSession::getUserB, userId)));
        long total = 0L;
        for (ChatSession session : sessions) {
            total += countUnreadInSession(userId, session.getId());
        }
        return total;
    }

    // ==================== 内部方法 ====================

    /**
     * 按申请ID查会话（不做权限校验，内部用）
     */
    private ChatSession getByRegistrationIdInternal(Long registrationId) {
        if (registrationId == null) {
            return null;
        }
        return chatSessionMapper.selectOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getRegistrationId, registrationId)
                .last("LIMIT 1"));
    }

    /**
     * 取一个「当前用户有权限访问」的会话，没有权限就抛异常
     */
    private ChatSession getOwnSession(Long userId, Long sessionId) {
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        if (sessionId == null) {
            throw new BusinessException("会话ID不能为空");
        }
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        if (!session.containsUser(userId)) {
            throw new BusinessException("无权访问该会话");
        }
        return session;
    }

    /**
     * 会话内最后一条消息
     */
    private ChatMessage getLastMessage(Long sessionId) {
        return chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByDesc(ChatMessage::getCreateTime)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT 1"));
    }

    /**
     * 我在某个会话里的未读数
     */
    private long countUnreadInSession(Long userId, Long sessionId) {
        if (userId == null || sessionId == null) {
            return 0L;
        }
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .ne(ChatMessage::getSenderId, userId)
                .eq(ChatMessage::getReadStatus, UNREAD);
        Long count = chatMessageMapper.selectCount(wrapper);
        return count == null ? 0L : count;
    }

    /**
     * PO -> VO，补发送人信息和时间文本
     */
    private ChatMessageVO toVo(ChatMessage po, Long currentUserId) {
        ChatMessageVO vo = ChatMessageVO.from(po, currentUserId);
        if (vo != null && vo.getCreateTime() != null) {
            vo.setCreateTimeText(DateUtil.formatDateTime(DateUtil.date(vo.getCreateTime())));
        }
        return vo;
    }
}
