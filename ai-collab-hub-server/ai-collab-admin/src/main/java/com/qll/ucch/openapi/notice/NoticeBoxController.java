package com.qll.ucch.openapi.notice;

import com.qll.ucch.exception.BusinessException;
import com.qll.ucch.models.PageResult;
import com.qll.ucch.models.Result;
import com.qll.ucch.models.dto.NoticeQueryDTO;
import com.qll.ucch.models.vo.NoticeUnreadVO;
import com.qll.ucch.models.vo.NoticeVO;
import com.qll.ucch.security.SecurityContext;
import com.qll.ucch.service.INoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.util.List;

/**
 * 消息盒接口。
 * 审核结果、收到的对接申请、系统通知、会话消息都汇总在这里，
 * 前端顶部小红点就是 /notice/info/unread 的数字。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@RestController
@RequestMapping("/notice/info")
@RequiredArgsConstructor
@Tag(name = "消息通知")
public class NoticeBoxController {

    private final INoticeService noticeService;

    @GetMapping("/list")
    @Operation(summary = "我的消息列表")
    public Result<PageResult<NoticeVO>> myNotice(NoticeQueryDTO query) {
        return Result.success(PageResult.of(noticeService.pageMyNotice(SecurityContext.requireUserId(), query)));
    }

    @GetMapping("/unread")
    @Operation(summary = "未读数量统计（总数 + 分类）")
    public Result<NoticeUnreadVO> unreadCount(
            @RequestParam(value = "msgType", required = false) Integer msgType) {
        Long userId = SecurityContext.requireUserId();
        // 前端只要某个分类的角标时走这个分支，省得算全部分类
        if (msgType != null) {
            NoticeUnreadVO vo = new NoticeUnreadVO();
            long count = noticeService.countUnreadByType(userId, msgType);
            vo.setTotal(count);
            if (msgType == 1) {
                vo.setAuditCount(count);
            } else if (msgType == 2) {
                vo.setApplyCount(count);
            } else if (msgType == 3) {
                vo.setSystemCount(count);
            } else if (msgType == 4) {
                vo.setChatCount(count);
            }
            return Result.success(vo);
        }
        return Result.success(noticeService.countUnread(userId));
    }

    @GetMapping("/unread/list")
    @Operation(summary = "我的全部未读消息（消息中心弹窗用）")
    public Result<List<NoticeVO>> unreadList() {
        return Result.success(noticeService.listUnread(SecurityContext.requireUserId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "消息详情")
    public Result<NoticeVO> detail(@PathVariable("id") Long id) {
        NoticeVO vo = noticeService.getDetail(SecurityContext.requireUserId(), id);
        if (vo == null) {
            throw new BusinessException("消息不存在，或者不是你的消息");
        }
        return Result.success(vo);
    }

    @PostMapping("/read-status")
    @Operation(summary = "标记已读（单条 / 批量）")
    public Result<Integer> markRead(@RequestBody ReadStatusRequest request) {
        Long userId = SecurityContext.requireUserId();
        if (request.getNoticeId() != null) {
            noticeService.markRead(userId, request.getNoticeId());
            return Result.success("已标记为已读", 1);
        }
        if (request.getNoticeIds() != null && !request.getNoticeIds().isEmpty()) {
            int count = 0;
            for (Long id : request.getNoticeIds()) {
                if (noticeService.markRead(userId, id)) {
                    count++;
                }
            }
            return Result.success("已标记 " + count + " 条为已读", count);
        }
        throw new BusinessException("请指定要标记的消息");
    }

    @PostMapping("/read-all")
    @Operation(summary = "全部标为已读")
    public Result<Integer> markAllRead(
            @RequestParam(value = "msgType", required = false) Integer msgType) {
        int count = noticeService.markAllRead(SecurityContext.requireUserId(), msgType);
        return Result.success("已把 " + count + " 条消息标为已读", count);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除消息")
    public Result<Boolean> delete(@PathVariable("id") Long id) {
        boolean ok = noticeService.deleteNotice(SecurityContext.requireUserId(), id);
        return Result.success(ok ? "已删除" : "删除失败", ok);
    }

    /** 标记已读入参：单条传 noticeId，批量传 noticeIds */
    @Data
    public static class ReadStatusRequest implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long noticeId;

        private List<Long> noticeIds;
    }
}
