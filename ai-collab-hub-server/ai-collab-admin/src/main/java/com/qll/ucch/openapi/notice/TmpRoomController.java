package com.qll.ucch.openapi.notice;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qll.ucch.exception.BusinessException;
import com.qll.ucch.models.PageResult;
import com.qll.ucch.models.Result;
import com.qll.ucch.models.dto.TmpRoomCreateDTO;
import com.qll.ucch.models.dto.TmpRoomMessageSendDTO;
import com.qll.ucch.models.vo.TmpRoomMessageVO;
import com.qll.ucch.models.vo.TmpRoomVO;
import com.qll.ucch.security.IgnoreAuth;
import com.qll.ucch.security.SecurityContext;
import com.qll.ucch.service.ITmpRoomService;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 临时沟通通道接口。
 *
 * 【和站内会话（ChatController）的区别】
 *   ChatController    长期会话，登录后在「站内沟通」页，能一直聊
 *   本 Controller      临时房间，链接形式（/tmp-room/{token}），到点自动失效
 * 两套并存、互不影响，前端入口也分开放（我的申请 / 消息中心）。
 *
 * 【安全边界】
 * 「按 token 查看房间」是免登录的（凭链接进入，像共享文档那样），
 * 但**发言必须登录且必须是参与人** —— 否则拿到链接的人就能冒充身份说话。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@RestController
@RequestMapping("/tmp/room")
@RequiredArgsConstructor
@Tag(name = "临时沟通通道")
public class TmpRoomController {

    private final ITmpRoomService tmpRoomService;
    private final SysUserService sysUserService;

    // ==================== 进入房间 ====================

    /**
     * 凭 token 进入房间。**免登录** —— 有链接就能看。
     * 前端路由 /tmp-room/{token} 调这个接口拿房间信息。
     */
    @IgnoreAuth
    @GetMapping("/enter")
    @Operation(summary = "凭链接进入临时通道（免登录可查看）")
    public Result<TmpRoomVO> enter(@RequestParam("token") String token) {
        Long viewerId = SecurityContext.getUserId();
        TmpRoomVO vo = tmpRoomService.enterByToken(token, viewerId);
        fillNames(java.util.Collections.singletonList(vo));
        return Result.success(vo);
    }

    /**
     * 查房间消息。同样凭 token，免登录可看。
     */
    @IgnoreAuth
    @GetMapping("/messages")
    @Operation(summary = "查临时通道消息（凭 token）")
    public Result<PageResult<TmpRoomMessageVO>> messages(
            @RequestParam("token") String token,
            @RequestParam(value = "pageNum", defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "100") Long pageSize) {
        // 先校验 token 可用，再拿房间ID查消息 —— 不要直接按 roomId 查，
        // 那样等于把「有没有权限看」这层绕过去了
        var room = tmpRoomService.requireUsableRoom(token);
        Long viewerId = SecurityContext.getUserId();
        IPage<TmpRoomMessageVO> page = tmpRoomService.pageMessages(room.getId(), viewerId, pageNum, pageSize);
        fillSenderNames(page.getRecords());
        return Result.success(PageResult.of(page));
    }

    // ==================== 发言 / 关闭 / 续期 ====================

    @PostMapping("/message")
    @Operation(summary = "在临时通道发言（需登录且须是参与人）")
    public Result<TmpRoomMessageVO> send(@Valid @RequestBody TmpRoomMessageSendDTO dto) {
        Long userId = SecurityContext.requireUserId();
        TmpRoomMessageVO vo = tmpRoomService.sendMessage(userId, dto.getRoomId(), dto.getContent());
        fillSenderNames(java.util.Collections.singletonList(vo));
        return Result.success("已发送", vo);
    }

    @PostMapping("/close")
    @Operation(summary = "关闭临时通道（参与人双方都能关）")
    public Result<Void> close(@RequestParam("roomId") Long roomId) {
        Long userId = SecurityContext.requireUserId();
        tmpRoomService.closeRoom(userId, roomId);
        return Result.success("通道已关闭", null);
    }

    @PostMapping("/extend")
    @Operation(summary = "延长临时通道有效期")
    public Result<TmpRoomVO> extend(@RequestParam("roomId") Long roomId,
                                    @RequestParam(value = "hours", required = false) Integer hours) {
        Long userId = SecurityContext.requireUserId();
        TmpRoomVO vo = tmpRoomService.extendRoom(userId, roomId, hours);
        fillNames(java.util.Collections.singletonList(vo));
        return Result.success("已续期", vo);
    }

    // ==================== 列表 / 查询 ====================

    @GetMapping("/my")
    @Operation(summary = "我参与的临时通道列表")
    public Result<List<TmpRoomVO>> myRooms() {
        Long userId = SecurityContext.requireUserId();
        List<TmpRoomVO> list = tmpRoomService.listMyRooms(userId);
        fillNames(list);
        return Result.success(list);
    }

    @GetMapping("/by-article/{articleId}")
    @Operation(summary = "按项目查当前有效的临时通道（前端判断要不要显示入口按钮）")
    public Result<TmpRoomVO> byArticle(@PathVariable("articleId") Long articleId) {
        TmpRoomVO vo = tmpRoomService.getActiveRoomByArticle(articleId);
        if (vo != null) {
            fillNames(java.util.Collections.singletonList(vo));
        }
        return Result.success(vo);
    }

    @GetMapping("/by-registration/{registrationId}")
    @Operation(summary = "按申请查当前有效的临时通道")
    public Result<TmpRoomVO> byRegistration(@PathVariable("registrationId") Long registrationId) {
        TmpRoomVO vo = tmpRoomService.getActiveRoomByRegistration(registrationId);
        if (vo != null) {
            fillNames(java.util.Collections.singletonList(vo));
        }
        return Result.success(vo);
    }

    @PostMapping("/create")
    @Operation(summary = "手动补开临时通道（申请通过时已自动开，这里是过期后重开的入口）")
    public Result<TmpRoomVO> create(@RequestBody TmpRoomCreateDTO dto) {
        Long userId = SecurityContext.requireUserId();
        if (dto.getRegistrationId() == null && dto.getArticleId() == null) {
            throw new BusinessException("请提供对接申请ID或项目ID");
        }
        TmpRoomVO vo = tmpRoomService.createRoom(userId, dto);
        fillNames(java.util.Collections.singletonList(vo));
        return Result.success("通道已开通", vo);
    }

    // ==================== 昵称回填（跨模块取数放这层） ====================

    private void fillNames(List<TmpRoomVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> ids = new HashSet<>();
        for (TmpRoomVO vo : list) {
            if (vo == null) {
                continue;
            }
            if (vo.getUserA() != null) {
                ids.add(vo.getUserA());
            }
            if (vo.getUserB() != null) {
                ids.add(vo.getUserB());
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> names = queryNames(new ArrayList<>(ids));
        for (TmpRoomVO vo : list) {
            if (vo == null) {
                continue;
            }
            vo.setUserAName(names.get(vo.getUserA()));
            vo.setUserBName(names.get(vo.getUserB()));
        }
    }

    private void fillSenderNames(List<TmpRoomMessageVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> ids = new HashSet<>();
        for (TmpRoomMessageVO vo : list) {
            if (vo != null && vo.getSenderId() != null) {
                ids.add(vo.getSenderId());
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> names = queryNames(new ArrayList<>(ids));
        for (TmpRoomMessageVO vo : list) {
            if (vo != null) {
                vo.setSenderName(names.get(vo.getSenderId()));
            }
        }
    }

    private Map<Long, String> queryNames(List<Long> ids) {
        Map<Long, String> map = new HashMap<>();
        try {
            List<com.qll.ucch.models.po.SysUser> users = sysUserService.listByIds(ids);
            if (users != null) {
                for (com.qll.ucch.models.po.SysUser u : users) {
                    map.put(u.getId(), u.getNickname() != null ? u.getNickname() : u.getAccount());
                }
            }
        } catch (Exception e) {
            // 回填失败不该让接口挂掉，顶多名字显示不出来
            log.warn("回填临时通道参与人昵称失败：{}", e.getMessage());
        }
        return map;
    }
}
