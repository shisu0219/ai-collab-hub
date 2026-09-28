package com.qll.ucch.openapi.notice;

import com.qll.ucch.exception.BusinessException;
import com.qll.ucch.models.PageResult;
import com.qll.ucch.models.Result;
import com.qll.ucch.models.dto.ChatMessageSendDTO;
import com.qll.ucch.models.dto.ChatSessionCreateDTO;
import com.qll.ucch.models.vo.ChatMessageVO;
import com.qll.ucch.models.po.SysUser;
import com.qll.ucch.models.vo.ChatSessionVO;
import com.qll.ucch.models.vo.RegistrationVO;
import com.qll.ucch.security.SecurityContext;
import com.qll.ucch.service.IBlogRegistrationService;
import com.qll.ucch.service.IChatService;
import com.qll.ucch.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 站内会话接口（新增功能）。
 * 对接申请通过之后双方在站内直接聊，不用再交换微信，聊天记录也留在平台上。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Tag(name = "站内会话")
public class ChatController {

    private final IChatService chatService;

    /** 查对接申请用，创建会话时要拿申请人和发布者两个ID */
    private final IBlogRegistrationService blogRegistrationService;

    /** 查用户昵称头像用，会话列表和消息里都要显示对方名字 */
    private final SysUserService sysUserService;

    @GetMapping("/session/list")
    @Operation(summary = "我的会话列表")
    public Result<List<ChatSessionVO>> mySession() {
        List<ChatSessionVO> list = chatService.listMySession(SecurityContext.requireUserId());
        fillTargetUser(list);
        return Result.success(list);
    }

    @GetMapping("/session/by-registration/{regId}")
    @Operation(summary = "按对接申请查会话")
    public Result<ChatSessionVO> byRegistration(@PathVariable("regId") Long regId) {
        Long userId = SecurityContext.requireUserId();
        ChatSessionVO vo = chatService.getByRegistrationId(regId, userId);
        if (vo == null) {
            // 还没建会话不算错，前端拿 null 提示「申请通过后就能聊了」
            return Result.success("该申请还没有会话，通过申请后会自动创建", null);
        }
        return Result.success(vo);
    }

    @GetMapping("/session/{sessionId}")
    @Operation(summary = "会话详情")
    public Result<ChatSessionVO> sessionDetail(@PathVariable("sessionId") Long sessionId) {
        ChatSessionVO vo = chatService.getSessionDetail(SecurityContext.requireUserId(), sessionId);
        if (vo == null) {
            throw new BusinessException("会话不存在，或者你不是会话参与者");
        }
        return Result.success(vo);
    }

    @PostMapping("/session")
    @Operation(summary = "创建会话")
    public Result<Long> createSession(@Valid @RequestBody ChatSessionCreateDTO dto) {
        Long userId = SecurityContext.requireUserId();

        // 前端一般只传 registrationId，参与人由后端从申请记录里查出来。
        // 申请记录里有申请人和文章发布者，这两个人就是会话双方。
        if (dto.getUserA() == null || dto.getUserB() == null) {
            if (dto.getRegistrationId() == null) {
                throw new BusinessException("请至少提供对接申请ID");
            }
            RegistrationVO reg = blogRegistrationService.getRegistrationDetail(
                    dto.getRegistrationId(), userId);
            if (reg == null) {
                throw new BusinessException("对接申请不存在，或者你没有权限");
            }
            dto.setUserA(reg.getUserId());
            dto.setUserB(reg.getArticleOwnerId());
            if (dto.getArticleId() == null) {
                dto.setArticleId(reg.getArticleId());
            }
        }

        // 普通用户只能把自己塞进会话里，管理员可以代开（运营排错用）
        if (!SecurityContext.isAdmin() && !userId.equals(dto.getUserA()) && !userId.equals(dto.getUserB())) {
            throw new BusinessException("只能创建与自己相关的会话");
        }
        return Result.success("会话已创建", chatService.createSession(dto));
    }

    @GetMapping("/message/list")
    @Operation(summary = "会话消息分页")
    public Result<PageResult<ChatMessageVO>> messageList(
            @RequestParam("sessionId") Long sessionId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        PageResult<ChatMessageVO> page = PageResult.of(chatService.pageMessage(
                SecurityContext.requireUserId(), sessionId, pageNum, pageSize));
        fillSenderName(page.getRecords());
        return Result.success(page);
    }

    @PostMapping("/message")
    @Operation(summary = "发送消息")
    public Result<ChatMessageVO> sendMessage(@Valid @RequestBody ChatMessageSendDTO dto) {
        // 内容和附件不能同时为空，DTO 自带校验方法，这里先挡一层
        try {
            dto.validateContent();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());
        }
        ChatMessageVO vo = chatService.sendMessage(SecurityContext.requireUserId(), dto);
        // 刚发出去这条也把昵称补上，前端好直接渲染
        fillSenderName(java.util.Collections.singletonList(vo));
        return Result.success("已发送", vo);
    }

    @PostMapping("/message/read")
    @Operation(summary = "标记会话消息已读")
    public Result<Integer> markRead(@RequestParam("sessionId") Long sessionId) {
        int count = chatService.markSessionRead(SecurityContext.requireUserId(), sessionId);
        return Result.success("已读", count);
    }

    @GetMapping("/message/unread")
    @Operation(summary = "未读消息数（指定会话 / 全部会话）")
    public Result<Map<String, Long>> unread(
            @RequestParam(value = "sessionId", required = false) Long sessionId) {
        Long userId = SecurityContext.requireUserId();
        Map<String, Long> data = new HashMap<>(4);
        if (sessionId != null) {
            data.put("sessionUnread", chatService.countSessionUnread(userId, sessionId));
        }
        data.put("totalUnread", chatService.countAllSessionUnread(userId));
        return Result.success(data);
    }

    // ==================== 展示信息补全 ====================

    /**
     * 给会话列表补上「对方」的昵称和头像。
     * 会话表里只存了两个 userId，昵称得去 sys_user 查，这里批量查一次避免 N+1。
     */
    private void fillTargetUser(List<ChatSessionVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> ids = list.stream()
                .map(ChatSessionVO::getTargetUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, SysUser> userMap = sysUserService.listByIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        for (ChatSessionVO vo : list) {
            SysUser u = userMap.get(vo.getTargetUserId());
            if (u != null) {
                vo.setTargetUserName(u.getNickname());
                vo.setTargetAvatar(u.getAvatar());
            }
        }
    }

    /**
     * 给消息列表补上发送人昵称。
     * 同一个会话里来来回回就那么两个人，用 Map 缓存一下省得重复查库。
     */
    private void fillSenderName(List<ChatMessageVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> ids = list.stream()
                .map(ChatMessageVO::getSenderId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, SysUser> userMap = sysUserService.listByIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        for (ChatMessageVO vo : list) {
            SysUser u = userMap.get(vo.getSenderId());
            if (u != null) {
                vo.setSenderName(u.getNickname());
                vo.setSenderAvatar(u.getAvatar());
            }
        }
    }
}
