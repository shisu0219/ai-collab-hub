package com.qll.ucch.openapi.user;

import com.qll.ucch.constance.FileConst;
import com.qll.ucch.exception.BusinessException;
import com.qll.ucch.models.Result;
import com.qll.ucch.models.dto.UpdateUserInfoDTO;
import com.qll.ucch.models.vo.FileInfoVO;
import com.qll.ucch.models.vo.UserInfoVO;
import com.qll.ucch.security.SecurityContext;
import com.qll.ucch.service.SysUserService;
import com.qll.ucch.utils.FileUploadUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
 * 个人资料接口。
 * 查自己的信息、改自己的信息、传头像，另外有个看别人主页的接口。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "个人资料")
public class UserController {

    private final SysUserService sysUserService;

    /** 文件上传根目录，配在 application.yaml 的 ai-collab.upload.root-dir */
    @Value("${ai-collab.upload.root-dir:}")
    private String uploadRootDir;

    // ==================== 我的信息 ====================

    @GetMapping("/info")
    @Operation(summary = "查询我的信息")
    public Result<UserInfoVO> myInfo() {
        return Result.success(sysUserService.getCurrentUserInfo(SecurityContext.requireUserId()));
    }

    @PutMapping("/info")
    @Operation(summary = "修改我的信息")
    public Result<Void> updateMyInfo(@Valid @RequestBody UpdateUserInfoDTO dto) {
        sysUserService.updateCurrentUserInfo(SecurityContext.requireUserId(), dto);
        return Result.success("保存成功", null);
    }

    /**
     * 传头像。
     * 前端 multipart 传过来，先落盘拿到访问地址，再把地址存进用户表，
     * 这样数据库里只存地址，换存储方案时不用动表结构。
     */
    @PostMapping("/info/avatar")
    @Operation(summary = "上传头像")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityContext.requireUserId();
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的头像");
        }

        FileInfoVO fileInfo = FileUploadUtils.upload(file, uploadRootDir, FileConst.DIR_AVATAR);
        sysUserService.updateAvatar(userId, fileInfo.getAccessUrl());

        log.info("用户 {} 更新头像：{}", userId, fileInfo.getAccessUrl());
        return Result.success("头像上传成功", fileInfo.getAccessUrl());
    }

    @GetMapping("/info/{id}")
    @Operation(summary = "查询指定用户信息（看他人主页）")
    public Result<UserInfoVO> userInfoById(@PathVariable("id") Long id) {
        UserInfoVO vo = sysUserService.getUserInfoById(id);
        if (vo == null) {
            throw new BusinessException("该用户不存在");
        }
        return Result.success(vo);
    }
}
