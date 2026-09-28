package com.qll.ucch.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qll.ucch.models.dto.ChatMessageSendDTO;
import com.qll.ucch.models.dto.ChatSessionCreateDTO;
import com.qll.ucch.models.vo.ChatMessageVO;
import com.qll.ucch.models.vo.ChatSessionVO;

import java.util.List;

/**
 * 站内会话服务（新增功能）。
 * <p>
 * 对接申请通过之后，双方在站内直接聊，不用再交换微信。一个对接申请对应
 * 一个会话（registration_id 唯一）。
 *
 * @author qll
 */
public interface IChatService {

    // ==================== 会话 ====================

    /**
     * 根据对接申请创建会话（双方用户ID）。
     * <p>
     * 幂等：如果这个申请已经有会话了，直接返回已有的，不重复建。
     * 一般由 blog 模块在审核通过时调用，也可以让前端在「开始沟通」时调。
     *
     * @param registrationId 对接申请ID
     * @param userA          参与人A（发起方）
     * @param userB          参与人B（接收方）
     * @param articleId      关联文章ID，可空
     * @return 会话ID
     */
    Long createSession(Long registrationId, Long userA, Long userB, Long articleId);

    /**
     * 用 DTO 创建会话
     */
    Long createSession(ChatSessionCreateDTO dto);

    /**
     * 查我的会话列表（按最后消息时间倒序），带对方信息、最后一条消息、未读数
     *
     * @param userId 当前登录用户ID
     * @return 会话列表
     */
    List<ChatSessionVO> listMySession(Long userId);

    /**
     * 按对接申请ID查会话，没有返回 null
     */
    ChatSessionVO getByRegistrationId(Long registrationId, Long currentUserId);

    /**
     * 会话详情（带权限校验：不是参与人不让看）
     */
    ChatSessionVO getSessionDetail(Long userId, Long sessionId);

    // ==================== 消息 ====================

    /**
     * 查会话内消息，分页。默认按时间倒序（新的在前），前端反过来渲染即可
     *
     * @param userId    当前登录用户ID，做权限校验
     * @param sessionId 会话ID
     * @param pageNum   页码
     * @param pageSize  每页条数
     */
    IPage<ChatMessageVO> pageMessage(Long userId, Long sessionId, Integer pageNum, Integer pageSize);

    /**
     * 发送消息。成功后顺带：
     * - 更新会话的 last_msg_time
     * - 给对方发一条 msg_type=4 的站内通知（消息中心能看到小红点）
     *
     * @param userId 发送人
     * @param dto    消息内容
     * @return 落库后的消息VO
     */
    ChatMessageVO sendMessage(Long userId, ChatMessageSendDTO dto);

    /**
     * 会话消息已读：把对方发给我的消息全部置为已读
     *
     * @return 更新条数
     */
    int markSessionRead(Long userId, Long sessionId);

    /**
     * 统计我在某个会话里的未读消息数
     */
    long countSessionUnread(Long userId, Long sessionId);

    /**
     * 统计我所有会话的未读消息总数（消息中心角标）
     */
    long countAllSessionUnread(Long userId);
}
