package com.qll.ucch.openapi.blog;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qll.ucch.exception.BusinessException;
import com.qll.ucch.models.PageResult;
import com.qll.ucch.models.Result;
import com.qll.ucch.models.dto.OpinionImportRowDTO;
import com.qll.ucch.models.dto.OpinionSaveDTO;
import com.qll.ucch.models.dto.OpinionScopeDTO;
import com.qll.ucch.models.vo.OpinionVO;
import com.qll.ucch.security.SecurityContext;
import com.qll.ucch.service.IBlogOpinionService;
import com.qll.ucch.service.INoticeService;
import com.qll.ucch.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 答辩意见接口。
 *
 * 意见由管理员上传，内容是答辩现场老师方/专业方给出的问题和意见。
 * 上传后会给小组全体成员发一条站内通知（发通知失败不影响上传本身）。
 *
 * 可见性的过滤在 Service 里做（列表和详情走同一套规则），
 * 这一层只负责取当前登录人、判是不是管理员，以及回填上传人昵称。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@RestController
@RequestMapping("/blog/opinion")
@RequiredArgsConstructor
@Tag(name = "答辩意见")
public class BlogOpinionController {

    private final IBlogOpinionService blogOpinionService;
    private final INoticeService noticeService;
    private final SysUserService sysUserService;

    // ==================== 查询（学生/老师都能调，返回内容按可见性裁剪） ====================

    @GetMapping("/list")
    @Operation(summary = "按小组查答辩意见列表")
    public Result<PageResult<OpinionVO>> pageByGroup(
            @RequestParam("groupId") Long groupId,
            @RequestParam(value = "pageNum", defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize) {
        Long viewerId = SecurityContext.getUserId();
        IPage<OpinionVO> page = blogOpinionService.pageByGroup(
                groupId, viewerId, SecurityContext.isAdmin(), pageNum, pageSize);
        fillUploaderNames(page.getRecords());
        return Result.success(PageResult.of(page));
    }

    @GetMapping("/list/by-article")
    @Operation(summary = "按项目查答辩意见列表")
    public Result<PageResult<OpinionVO>> pageByArticle(
            @RequestParam("articleId") Long articleId,
            @RequestParam(value = "pageNum", defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize) {
        Long viewerId = SecurityContext.getUserId();
        IPage<OpinionVO> page = blogOpinionService.pageByArticle(
                articleId, viewerId, SecurityContext.isAdmin(), pageNum, pageSize);
        fillUploaderNames(page.getRecords());
        return Result.success(PageResult.of(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "答辩意见详情")
    public Result<OpinionVO> detail(@PathVariable("id") Long id) {
        Long viewerId = SecurityContext.getUserId();
        OpinionVO vo = blogOpinionService.getOpinionDetail(id, viewerId, SecurityContext.isAdmin());
        fillUploaderNames(java.util.Collections.singletonList(vo));
        return Result.success(vo);
    }

    @GetMapping("/count")
    @Operation(summary = "某小组可见的答辩意见条数")
    public Result<Integer> count(@RequestParam("groupId") Long groupId) {
        Long viewerId = SecurityContext.getUserId();
        return Result.success(blogOpinionService.countVisible(groupId, viewerId, SecurityContext.isAdmin()));
    }

    // ==================== 组长设置可见性 ====================

    @PutMapping("/scope")
    @Operation(summary = "设置答辩意见可见性（组长）")
    public Result<Void> setScope(@Valid @RequestBody OpinionScopeDTO dto) {
        Long operator = SecurityContext.requireUserId();
        blogOpinionService.setScope(operator, dto.getGroupId(), dto.getOpinionId(), dto.getScope());
        return Result.success("设置成功", null);
    }

    // ==================== 管理员：上传 / 修改 / 删除 ====================

    @PostMapping("/admin/save")
    @Operation(summary = "管理员上传或修改答辩意见")
    public Result<Long> save(@Valid @RequestBody OpinionSaveDTO dto) {
        requireAdmin();
        Long adminId = SecurityContext.requireUserId();
        Long id = blogOpinionService.saveOpinion(adminId, dto);
        // 上传后给组内成员发通知
        notifyGroupMembers(dto.getGroupId(), dto.getTitle());
        return Result.success("上传成功", id);
    }

    /**
     * 批量导入。前端把 Excel 解析成数组后一次提交，不用手工一条条敲。
     */
    @PostMapping("/admin/import")
    @Operation(summary = "管理员批量导入答辩意见（Excel 解析后提交）")
    public Result<Map<String, Object>> importRows(@RequestParam("groupId") Long groupId,
                                                  @RequestBody List<OpinionImportRowDTO> rows) {
        requireAdmin();
        Long adminId = SecurityContext.requireUserId();
        int ok = blogOpinionService.importOpinions(adminId, groupId, rows);
        notifyGroupMembers(groupId, "批量导入 " + ok + " 条");
        Map<String, Object> data = new HashMap<>();
        data.put("count", ok);
        return Result.success("成功导入 " + ok + " 条", data);
    }

    @PutMapping("/admin/scope")
    @Operation(summary = "管理员设置可见性（管理员也能设，便于处理异常情况）")
    public Result<Void> adminSetScope(@Valid @RequestBody OpinionScopeDTO dto) {
        requireAdmin();
        Long adminId = SecurityContext.requireUserId();
        blogOpinionService.setScope(adminId, dto.getGroupId(), dto.getOpinionId(), dto.getScope());
        return Result.success("设置成功", null);
    }

    @DeleteMapping("/admin/{id}")
    @Operation(summary = "管理员删除答辩意见")
    public Result<Void> delete(@PathVariable("id") Long id) {
        requireAdmin();
        Long adminId = SecurityContext.requireUserId();
        blogOpinionService.deleteOpinion(adminId, id);
        return Result.success("已删除", null);
    }

    // ==================== 内部 ====================

    private void requireAdmin() {
        if (!SecurityContext.isAdmin()) {
            throw new BusinessException("只有管理员可以上传答辩意见");
        }
    }

    /**
     * 给小组全体成员发通知。
     * 照抄 BlogArticleServiceImpl.sendNoticeQuietly 的做法：发通知失败只记日志，
     * 不能因为通知挂了就让「上传意见」这个主流程失败。
     */
    private void notifyGroupMembers(Long groupId, String title) {
        if (groupId == null) {
            return;
        }
        try {
            List<Long> memberIds = blogOpinionService.listGroupMemberIds(groupId);
            if (memberIds.isEmpty()) {
                return;
            }
            String text = "你所在的小组有新的答辩意见上传：" + (title == null ? "" : title);
            for (Long uid : memberIds) {
                noticeService.sendNotice(uid, "答辩意见", text, 4);
            }
        } catch (Exception e) {
            log.warn("发送答辩意见通知失败（不影响上传）：{}", e.getMessage());
        }
    }

    /**
     * 回填上传人昵称（跨模块取数放这一层）。
     * 一次性批量查用户表，不要在循环里单条查 —— 那会变成 N+1。
     */
    private void fillUploaderNames(List<OpinionVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> uploaderIds = new ArrayList<>();
        for (OpinionVO vo : list) {
            if (vo != null && vo.getUploaderId() != null) {
                uploaderIds.add(vo.getUploaderId());
            }
        }
        if (uploaderIds.isEmpty()) {
            return;
        }
        try {
            Map<Long, String> nameMap = new HashMap<>();
            List<com.qll.ucch.models.po.SysUser> users = sysUserService.listByIds(uploaderIds);
            if (users != null) {
                for (com.qll.ucch.models.po.SysUser u : users) {
                    nameMap.put(u.getId(), u.getNickname() != null ? u.getNickname() : u.getAccount());
                }
            }
            for (OpinionVO vo : list) {
                if (vo != null && vo.getUploaderId() != null) {
                    vo.setUploaderName(nameMap.get(vo.getUploaderId()));
                }
            }
        } catch (Exception e) {
            // 回填失败不该让接口挂掉，页面顶多少个名字
            log.warn("回填上传人昵称失败：{}", e.getMessage());
        }
    }
}
