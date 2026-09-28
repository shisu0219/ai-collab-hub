package com.qll.ucch.openapi.blog;

import com.qll.ucch.constance.FileConst;
import com.qll.ucch.exception.BusinessException;
import com.qll.ucch.models.PageResult;
import com.qll.ucch.models.Result;
import com.qll.ucch.models.dto.CollabProgressDTO;
import com.qll.ucch.models.dto.RegistrationHandleDTO;
import com.qll.ucch.models.dto.RegistrationQueryDTO;
import com.qll.ucch.models.dto.RegistrationSubmitDTO;
import com.qll.ucch.models.vo.FileInfoVO;
import com.qll.ucch.models.po.SysUser;
import com.qll.ucch.models.vo.RegistrationVO;
import com.qll.ucch.security.SecurityContext;
import com.qll.ucch.service.IBlogRegistrationService;
import com.qll.ucch.service.SysUserService;
import com.qll.ucch.utils.FileUploadUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 对接申请接口。
 * 学生看到老师需求、老师看到学生项目，点「我要对接」就走这里提交，
 * 之后双方在「我的对接」里谈进度。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@RestController
@RequestMapping("/blog/registration")
@RequiredArgsConstructor
@Tag(name = "对接申请")
public class BlogRegistrationController {

    private final IBlogRegistrationService blogRegistrationService;

    /** 查申请人昵称用。blog 模块拿不到 sys 的用户表，所以在 Controller 层补。 */
    private final SysUserService sysUserService;

    @Value("${ai-collab.upload.root-dir:}")
    private String uploadRootDir;

    @PostMapping
    @Operation(summary = "提交对接申请")
    public Result<Long> submit(@Valid @RequestBody RegistrationSubmitDTO dto) {
        Long applicantId = SecurityContext.requireUserId();
        // 学生和老师的申请表单字段不一样（学生填年级班级擅长，老师填为什么感兴趣），
        // Service 里要用这个决定校验口径，所以在这里判断好传进去。
        boolean isTeacher = "TEACHER".equalsIgnoreCase(SecurityContext.getRoleCode());
        Long registrationId = blogRegistrationService.submitRegistration(dto, applicantId, isTeacher);
        return Result.success("申请已提交，等待对方处理", registrationId);
    }

    @GetMapping("/list")
    @Operation(summary = "对接申请列表（我发出的 / 我收到的）")
    public Result<PageResult<RegistrationVO>> list(@Valid RegistrationQueryDTO query) {
        Long userId = SecurityContext.requireUserId();
        PageResult<RegistrationVO> page =
                PageResult.of(blogRegistrationService.pageRegistration(query, userId));
        fillApplicantName(page.getRecords());
        return Result.success(page);
    }

    /**
     * 把申请人昵称补到 VO 上。
     * <p>
     * blog 模块查不到 sys_user 表，所以 service 层留了空，在这里统一补。
     * 一次性批量查，不要在循环里单条查库。
     */
    private void fillApplicantName(java.util.List<RegistrationVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        java.util.List<Long> userIds = list.stream()
                .map(RegistrationVO::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        if (userIds.isEmpty()) {
            return;
        }
        java.util.Map<Long, String> nameMap = new java.util.HashMap<>(userIds.size());
        for (SysUser u : sysUserService.listByIds(userIds)) {
            // 优先用昵称，没有就用账号兜底
            nameMap.put(u.getId(),
                    cn.hutool.core.util.StrUtil.isNotBlank(u.getNickname())
                            ? u.getNickname() : u.getAccount());
        }
        for (RegistrationVO vo : list) {
            String name = nameMap.get(vo.getUserId());
            if (name != null) {
                vo.setApplicantName(name);
            }
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "对接申请详情")
    public Result<RegistrationVO> detail(@PathVariable("id") Long id) {
        RegistrationVO vo = blogRegistrationService.getRegistrationDetail(id, SecurityContext.requireUserId());
        if (vo == null) {
            throw new BusinessException("申请不存在，或者你没有权限查看");
        }
        fillApplicantName(java.util.Collections.singletonList(vo));
        return Result.success(vo);
    }

    @PostMapping("/review")
    @Operation(summary = "处理对接申请（通过 / 拒绝）")
    public Result<Void> review(@Valid @RequestBody RegistrationHandleDTO dto) {
        blogRegistrationService.handleRegistration(dto, SecurityContext.requireUserId());
        String tip = dto.getPass() != null && dto.getPass() == 1 ? "已通过该申请" : "已拒绝该申请";
        return Result.success(tip, null);
    }

    @PutMapping("/progress")
    @Operation(summary = "更新对接进度")
    public Result<Void> updateProgress(@Valid @RequestBody CollabProgressDTO dto) {
        blogRegistrationService.updateCollabProgress(dto, SecurityContext.requireUserId());
        return Result.success("进度已更新", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "撤回我发出的申请")
    public Result<Void> cancel(@PathVariable("id") Long id) {
        blogRegistrationService.cancelRegistration(id, SecurityContext.requireUserId());
        return Result.success("已撤回", null);
    }

    /**
     * 上传对接材料。
     * 申请时可以带方案、简历这类附件，跟文章附件走同一个目录，省一套存储策略。
     */
    @PostMapping("/upload")
    @Operation(summary = "上传对接材料附件")
    public Result<FileInfoVO> uploadAttachment(@RequestParam("file") MultipartFile file) {
        SecurityContext.requireUserId();
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        FileInfoVO fileInfo = FileUploadUtils.upload(file, uploadRootDir, FileConst.DIR_ARTICLE);
        return Result.success("上传成功", fileInfo);
    }
}
