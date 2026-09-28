package com.qll.ucch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qll.ucch.exception.BusinessException;
import com.qll.ucch.mapper.TmpRoomMapper;
import com.qll.ucch.mapper.TmpRoomMessageMapper;
import com.qll.ucch.models.dto.TmpRoomCreateDTO;
import com.qll.ucch.models.po.TmpRoom;
import com.qll.ucch.models.po.TmpRoomMessage;
import com.qll.ucch.models.vo.TmpRoomMessageVO;
import com.qll.ucch.models.vo.TmpRoomVO;
import com.qll.ucch.service.ITmpRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 临时沟通房间服务实现。
 *
 * 【几个关键设计，改之前先看】
 *
 * 1. room_token 用 SecureRandom 生成 24 字节再 Base64 编码（URL 安全字符集），
 *    不可猜、不会撞。**不要改成自增ID或时间戳拼的**，那等于把房间大门敞开。
 *
 * 2. 判断房间可用必须 status + expireTime 一起看：
 *    - status=0 表示被手动关闭（可能还没到点）
 *    - expireTime < now 表示到点自动失效（status 可能还是 1）
 *    **只看一个就会漏。**
 *
 * 3. 进房间是免登录的（靠 token），但**发言必须登录且必须是参与人**。
 *    不然拿到链接的人都能冒充身份发言。
 *
 * 4. 房间过期后消息不删，只是接口不再返回 —— 追溯用得到。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TmpRoomServiceImpl extends ServiceImpl<TmpRoomMapper, TmpRoom>
        implements ITmpRoomService {

    /** 默认有效期（小时）。聊天这事儿有个两三天的窗口足够了。 */
    public static final int DEFAULT_HOURS = 72;

    /** 状态：有效 */
    private static final int ST_ACTIVE = 1;
    /** 状态：已关闭 */
    private static final int ST_CLOSED = 0;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final TmpRoomMessageMapper messageMapper;

    // ==================== 建房间 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TmpRoomVO createRoom(Long operatorId, TmpRoomCreateDTO dto) {
        if (dto == null || (dto.getRegistrationId() == null && dto.getArticleId() == null)) {
            throw new BusinessException("请提供对接申请ID或项目ID");
        }
        // 已有有效房间就直接复用，别开一堆（否则对方会给一串链接，没法用）
        TmpRoom exist = dto.getRegistrationId() != null
                ? findActiveByRegistration(dto.getRegistrationId())
                : findActiveByArticle(dto.getArticleId());
        if (exist != null) {
            log.info("已有有效房间 {}，直接复用", exist.getId());
            return toVO(exist, operatorId);
        }

        int hours = (dto.getHours() == null || dto.getHours() < 1) ? DEFAULT_HOURS : dto.getHours();

        // 参与人：优先用调用方传进来的（申请通过时后端带出来），
        // 手动补开时才退回「operatorId + 申请的双方」
        Long userA = dto.getUserA() != null ? dto.getUserA() : operatorId;
        Long userB = dto.getUserB() != null ? dto.getUserB() : operatorId;
        if (userA == null || userB == null) {
            throw new BusinessException("参与人信息不全，无法开通道");
        }

        TmpRoom room = new TmpRoom();
        room.setRoomToken(newToken());
        room.setRegistrationId(dto.getRegistrationId());
        room.setArticleId(dto.getArticleId());
        room.setUserA(userA);
        room.setUserB(userB);
        room.setTitle(dto.getTitle());
        room.setStatus(ST_ACTIVE);
        room.setExpireTime(LocalDateTime.now().plusHours(hours));
        save(room);

        log.info("用户 {} 开了临时房间 {}（token 长度 {}），有效期 {} 小时",
                operatorId, room.getId(), room.getRoomToken().length(), hours);
        return toVO(room, operatorId);
    }

    /**
     * 生成随机令牌。URL 安全字符集，不会被转义搞坏。
     */
    private String newToken() {
        byte[] buf = new byte[24];
        RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    // ==================== 查询 ====================

    @Override
    public TmpRoomVO getActiveRoomByArticle(Long articleId) {
        TmpRoom room = findActiveByArticle(articleId);
        return room == null ? null : toVO(room, null);
    }

    @Override
    public TmpRoomVO getActiveRoomByRegistration(Long registrationId) {
        TmpRoom room = findActiveByRegistration(registrationId);
        return room == null ? null : toVO(room, null);
    }

    @Override
    public TmpRoomVO enterByToken(String token, Long viewerId) {
        TmpRoom room = requireUsableRoom(token);
        return toVO(room, viewerId);
    }

    @Override
    public List<TmpRoomVO> listMyRooms(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<TmpRoom> rooms = list(new LambdaQueryWrapper<TmpRoom>()
                .and(w -> w.eq(TmpRoom::getUserA, userId).or().eq(TmpRoom::getUserB, userId))
                .orderByDesc(TmpRoom::getCreateTime));
        if (rooms.isEmpty()) {
            return Collections.emptyList();
        }
        return rooms.stream().map(r -> toVO(r, userId)).collect(Collectors.toList());
    }

    @Override
    public boolean isParticipant(Long roomId, Long userId) {
        if (roomId == null || userId == null) {
            return false;
        }
        TmpRoom r = getById(roomId);
        return r != null && (userId.equals(r.getUserA()) || userId.equals(r.getUserB()));
    }

    @Override
    public TmpRoom requireUsableRoom(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException("通道链接无效");
        }
        TmpRoom room = getOne(new LambdaQueryWrapper<TmpRoom>()
                .eq(TmpRoom::getRoomToken, token)
                .last("limit 1"));
        if (room == null) {
            throw new BusinessException("通道不存在，链接可能有误");
        }
        if (room.getStatus() == null || room.getStatus() != ST_ACTIVE) {
            throw new BusinessException("这个临时通道已经关闭了");
        }
        if (room.getExpireTime() == null || room.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("这个临时通道已过期，如需继续沟通请重新开通");
        }
        return room;
    }

    // ==================== 消息 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TmpRoomMessageVO sendMessage(Long senderId, Long roomId, String content) {
        if (senderId == null) {
            throw new BusinessException("请先登录再发言");
        }
        TmpRoom room = getById(roomId);
        if (room == null) {
            throw new BusinessException("通道不存在");
        }
        // 用 token 校验那套逻辑统一判断可用性，避免这里漏掉某个条件
        requireUsableRoom(room.getRoomToken());

        if (!senderId.equals(room.getUserA()) && !senderId.equals(room.getUserB())) {
            throw new BusinessException("你不在这个通道里，不能发言");
        }
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("消息内容不能为空");
        }

        TmpRoomMessage msg = new TmpRoomMessage();
        msg.setRoomId(roomId);
        msg.setSenderId(senderId);
        msg.setContent(content);
        messageMapper.insert(msg);

        TmpRoomMessageVO vo = toMsgVO(msg, senderId);
        return vo;
    }

    @Override
    public IPage<TmpRoomMessageVO> pageMessages(Long roomId, Long viewerId, Long pageNum, Long pageSize) {
        TmpRoom room = getById(roomId);
        if (room == null) {
            throw new BusinessException("通道不存在");
        }
        requireUsableRoom(room.getRoomToken());

        Page<TmpRoomMessage> page = new Page<>(
                pageNum == null || pageNum < 1 ? 1 : pageNum,
                pageSize == null || pageSize < 1 ? 50 : Math.min(pageSize, 200));
        IPage<TmpRoomMessage> raw = messageMapper.selectPage(page,
                new LambdaQueryWrapper<TmpRoomMessage>()
                        .eq(TmpRoomMessage::getRoomId, roomId)
                        .orderByAsc(TmpRoomMessage::getCreateTime));

        List<TmpRoomMessageVO> out = new ArrayList<>();
        for (TmpRoomMessage m : raw.getRecords()) {
            out.add(toMsgVO(m, viewerId));
        }
        Page<TmpRoomMessageVO> result = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        result.setRecords(out);
        return result;
    }

    // ==================== 关闭 / 续期 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeRoom(Long operatorId, Long roomId) {
        TmpRoom room = getById(roomId);
        if (room == null) {
            throw new BusinessException("通道不存在");
        }
        if (!operatorId.equals(room.getUserA()) && !operatorId.equals(room.getUserB())) {
            throw new BusinessException("只有通道内的人可以关闭");
        }
        room.setStatus(ST_CLOSED);
        updateById(room);
        log.info("用户 {} 关闭了临时房间 {}", operatorId, roomId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TmpRoomVO extendRoom(Long operatorId, Long roomId, Integer hours) {
        TmpRoom room = getById(roomId);
        if (room == null) {
            throw new BusinessException("通道不存在");
        }
        if (!operatorId.equals(room.getUserA()) && !operatorId.equals(room.getUserB())) {
            throw new BusinessException("只有通道内的人可以续期");
        }
        int h = (hours == null || hours < 1) ? DEFAULT_HOURS : hours;
        // 从「现在」还是「原到期时间」往后延？取较晚的那个，避免过期房续期后反而变短
        LocalDateTime base = (room.getExpireTime() != null && room.getExpireTime().isAfter(LocalDateTime.now()))
                ? room.getExpireTime()
                : LocalDateTime.now();
        room.setExpireTime(base.plusHours(h));
        room.setStatus(ST_ACTIVE);   // 续期顺便把关闭状态打开（如果之前关了）
        updateById(room);
        log.info("用户 {} 把临时房间 {} 续了 {} 小时", operatorId, roomId, h);
        return toVO(room, operatorId);
    }

    // ==================== 内部 ====================

    private TmpRoom findActiveByArticle(Long articleId) {
        if (articleId == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<TmpRoom>()
                .eq(TmpRoom::getArticleId, articleId)
                .eq(TmpRoom::getStatus, ST_ACTIVE)
                .gt(TmpRoom::getExpireTime, LocalDateTime.now())
                .orderByDesc(TmpRoom::getCreateTime)
                .last("limit 1"));
    }

    private TmpRoom findActiveByRegistration(Long registrationId) {
        if (registrationId == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<TmpRoom>()
                .eq(TmpRoom::getRegistrationId, registrationId)
                .eq(TmpRoom::getStatus, ST_ACTIVE)
                .gt(TmpRoom::getExpireTime, LocalDateTime.now())
                .orderByDesc(TmpRoom::getCreateTime)
                .last("limit 1"));
    }

    private TmpRoomVO toVO(TmpRoom r, Long viewerId) {
        TmpRoomVO vo = new TmpRoomVO();
        vo.setId(r.getId());
        vo.setRoomToken(r.getRoomToken());
        vo.setAccessPath("/tmp-room/" + r.getRoomToken());
        vo.setRegistrationId(r.getRegistrationId());
        vo.setArticleId(r.getArticleId());
        vo.setTitle(r.getTitle());
        vo.setUserA(r.getUserA());
        vo.setUserB(r.getUserB());
        vo.setStatus(r.getStatus());
        vo.setExpireTime(r.getExpireTime());
        vo.setCreateTime(r.getCreateTime());

        boolean expired = r.getExpireTime() == null || r.getExpireTime().isBefore(LocalDateTime.now());
        vo.setExpired(r.getStatus() == null || r.getStatus() != ST_ACTIVE || expired);
        if (expired) {
            vo.setRemainingHours(0L);
        } else {
            long mins = Duration.between(LocalDateTime.now(), r.getExpireTime()).toMinutes();
            // 向上取整，剩 30 分钟显示 1 小时，别显示 0 让人以为已经没了
            vo.setRemainingHours((mins + 59) / 60);
        }
        // 昵称由 admin 层回填
        return vo;
    }

    private TmpRoomMessageVO toMsgVO(TmpRoomMessage m, Long viewerId) {
        TmpRoomMessageVO vo = new TmpRoomMessageVO();
        vo.setId(m.getId());
        vo.setRoomId(m.getRoomId());
        vo.setSenderId(m.getSenderId());
        vo.setContent(m.getContent());
        vo.setAttachments(m.getAttachments());
        vo.setCreateTime(m.getCreateTime());
        vo.setMine(viewerId != null && viewerId.equals(m.getSenderId()));
        // 发送人昵称由 admin 层回填
        return vo;
    }
}
