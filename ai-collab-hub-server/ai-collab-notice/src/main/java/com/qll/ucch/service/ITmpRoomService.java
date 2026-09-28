package com.qll.ucch.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qll.ucch.models.dto.TmpRoomCreateDTO;
import com.qll.ucch.models.po.TmpRoom;
import com.qll.ucch.models.vo.TmpRoomMessageVO;
import com.qll.ucch.models.vo.TmpRoomVO;

import java.util.List;

/**
 * 临时沟通房间服务。
 *
 * 用途：申请通过后，给双方开一个**有时效的**沟通通道，
 * 不用先互换微信，也能先把事情聊清楚。
 *
 * 【和站内会话（IChatService）的关系】
 * 两套并存、互不影响：
 *   IChatService  长期会话，登录后在「站内沟通」里，能一直聊
 *   ITmpRoomService  临时房间，链接形式，到点失效
 * 不要试图把两者合并 —— 语义不同（正式合作 vs 先聊聊看）。
 *
 * 【校验纪律】
 * 判断房间「能不能用」必须同时看 status 和 expireTime，
 * 只看一个会漏：房间可能被手动关闭但没过期，也可能没关但已过期。
 *
 * @author 人工智能学院双创平台
 */
public interface ITmpRoomService extends IService<TmpRoom> {

    /**
     * 开一个临时房间（一般由申请通过时自动调）。
     *
     * @param operatorId 操作人（申请人或发布方都行）
     * @param dto        创建入参
     * @return 房间信息（含拼好的访问路径）
     */
    TmpRoomVO createRoom(Long operatorId, TmpRoomCreateDTO dto);

    /**
     * 按项目ID查当前有效的房间（没有则返回 null）。
     * 前端用它判断「该不该显示『进入临时通道』按钮」。
     */
    TmpRoomVO getActiveRoomByArticle(Long articleId);

    /**
     * 按申请ID查当前有效的房间。
     */
    TmpRoomVO getActiveRoomByRegistration(Long registrationId);

    /**
     * 凭 token 进房间。**这是免登录入口**，所以要严格校验 token + 状态 + 时效。
     *
     * @param token    房间令牌
     * @param viewerId 当前登录人ID，未登录传 null（未登录只能看，不能发言）
     */
    TmpRoomVO enterByToken(String token, Long viewerId);

    /**
     * 发消息进房间。
     */
    TmpRoomMessageVO sendMessage(Long senderId, Long roomId, String content);

    /**
     * 查房间消息（只返回未过期的房间的消息）。
     */
    IPage<TmpRoomMessageVO> pageMessages(Long roomId, Long viewerId, Long pageNum, Long pageSize);

    /**
     * 关闭房间（参与人双方都能关）。
     */
    void closeRoom(Long operatorId, Long roomId);

    /**
     * 延长有效期（续期）。只有参与人能续。
     */
    TmpRoomVO extendRoom(Long operatorId, Long roomId, Integer hours);

    /**
     * 我参与的房间列表。
     */
    List<TmpRoomVO> listMyRooms(Long userId);

    /**
     * 判断某人是不是这个房间的参与人。
     */
    boolean isParticipant(Long roomId, Long userId);

    /**
     * 检查房间是否可用（未关闭 + 未过期）。不可用就抛业务异常。
     */
    TmpRoom requireUsableRoom(String token);
}
