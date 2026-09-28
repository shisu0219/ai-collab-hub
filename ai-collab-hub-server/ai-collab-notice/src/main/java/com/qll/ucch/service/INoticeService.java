package com.qll.ucch.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qll.ucch.models.dto.NoticeQueryDTO;
import com.qll.ucch.models.dto.NoticeSendDTO;
import com.qll.ucch.models.vo.NoticeUnreadVO;
import com.qll.ucch.models.vo.NoticeVO;

import java.util.List;

/**
 * 消息通知服务。
 * <p>
 * 这个接口会被别的模块（比如 blog 审核完发通知）调用，所以 sendNotice 的签名
 * 定死了，不要乱改：
 * <pre>
 * void sendNotice(Long userId, String title, String content, Integer msgType)
 * </pre>
 *
 * @author qll
 */
public interface INoticeService {

    // ==================== 发送 ====================

    /**
     * 发送一条站内消息（对外唯一入口，签名固定，别动）
     *
     * @param userId  接收人用户ID
     * @param title   消息标题
     * @param content 消息内容
     * @param msgType 消息分类：1审核结果 2收到申请 3系统通知 4会话消息
     */
    void sendNotice(Long userId, String title, String content, Integer msgType);

    /**
     * 发送一条**带关联对象**的站内消息。
     * <p>
     * 和上面那个的区别：多一个 refId，用来记「这条通知是关于哪个对象的」。
     * 目前只有会话消息（msgType=4）用得到，refId 存会话ID。
     * <p>
     * 【为什么要单独开一个方法而不是改原签名】
     * 原 sendNotice 被 blog 模块（审核完发通知）调用，签名在设计上是锁死的。
     * 加参数会让 blog 模块编译不过，也会破坏模块间的约定。所以用重载。
     *
     * @param refId 关联对象ID（会话消息存会话ID；其它类型传 null）
     */
    void sendNotice(Long userId, String title, String content, Integer msgType, Long refId);

    /**
     * 按关联对象标记已读。
     * <p>
     * 用于「读完某个会话，把消息中心里关于这个会话的通知也标已读」，
     * 实现两个红点联动。别的会话的通知不受影响。
     *
     * @param userId 当前用户
     * @param refId  关联对象ID（会话ID）
     * @return 影响的条数
     */
    int markReadByRef(Long userId, Long refId);

    /**
     * 用 DTO 发送，内部还是走 sendNotice
     */
    void sendNotice(NoticeSendDTO dto);

    /**
     * 群发，给多个用户发同一条消息（比如系统维护公告）
     */
    void sendNoticeBatch(List<Long> userIds, String title, String content, Integer msgType);

    // ==================== 查询 ====================

    /**
     * 我的消息分页列表。
     * <p>
     * 新增能力：支持 msgType 分类过滤 + title 模糊搜索 + readStatus 过滤
     *
     * @param userId 当前登录用户ID
     * @param query  查询条件
     */
    IPage<NoticeVO> pageMyNotice(Long userId, NoticeQueryDTO query);

    /**
     * 我的全部未读消息（不分页，用于消息中心弹窗，最多内部限制 100 条）
     */
    List<NoticeVO> listUnread(Long userId);

    /**
     * 消息详情。顺便校验这条消息是不是本人的
     */
    NoticeVO getDetail(Long userId, Long noticeId);

    // ==================== 状态变更 ====================

    /**
     * 标记单条已读
     *
     * @return 是否更新成功
     */
    boolean markRead(Long userId, Long noticeId);

    /**
     * 全部标记已读。msgType 传 null 表示所有分类都标记
     *
     * @return 影响的条数
     */
    int markAllRead(Long userId, Integer msgType);

    /**
     * 删除单条消息（逻辑删除，delete_time 打时间戳）
     */
    boolean deleteNotice(Long userId, Long noticeId);

    /**
     * 批量删除（逻辑删除）
     */
    int deleteBatch(Long userId, List<Long> noticeIds);

    /**
     * 清空我的消息（逻辑删除全部），可选按分类清
     */
    int clearAll(Long userId, Integer msgType);

    // ==================== 统计 ====================

    /**
     * 未读数量统计：总数 + 各分类数量
     */
    NoticeUnreadVO countUnread(Long userId);

    /**
     * 某个分类的未读数（简单场景用）
     */
    long countUnreadByType(Long userId, Integer msgType);
}
