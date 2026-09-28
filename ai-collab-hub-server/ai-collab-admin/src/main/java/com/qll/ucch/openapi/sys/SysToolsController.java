package com.qll.ucch.openapi.sys;

import com.qll.ucch.exception.BusinessException;
import com.qll.ucch.models.Result;
import com.qll.ucch.security.SecurityContext;
import com.qll.ucch.utils.FileDownloadUtils;
import com.qll.ucch.utils.FileUploadUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * 系统小工具接口。
 * <p>
 * 这里放三样东西：
 * 1. 拒绝理由模板 —— 审核时管理员点一下就填进意见框，省得每次手打；
 * 2. 下载文件 —— 附件在浏览器里下下来，中文名不乱码；
 * 3. 预览文件 —— 图片、PDF 直接内联打开，不用先下载。
 * <p>
 * 模板数据量很小而且基本不变，直接写在代码里，没有单独建表——建张表还得配
 * 增删改接口和维护页面，收益不大。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@RestController
@RequestMapping("/sys/tool")
@RequiredArgsConstructor
@Tag(name = "系统工具")
public class SysToolsController {

    @Value("${ai-collab.upload.root-dir:}")
    private String uploadRootDir;

    // ==================== 拒绝理由模板 ====================

    /** 用户审核用的拒绝理由 */
    private static final List<RejectTemplate> USER_TEMPLATES = List.of(
            new RejectTemplate("user_credential_blur", "材料不清晰",
                    "您上传的证明材料图片模糊看不清，请重新上传清晰的证件照片或扫描件。"),
            new RejectTemplate("user_credential_insufficient", "材料不完整",
                    "证明材料不完整，缺少必要的资质文件，请补齐后重新提交。"),
            new RejectTemplate("user_info_mismatch", "信息不一致",
                    "填写的账号信息与证明材料上的信息不一致，请核对后重新提交。"),
            new RejectTemplate("user_teacher_unverified", "教师资格未核实",
                    "教师身份信息无法核实，请提供有效的教师资格证明或工作证明后重新提交。"),
            new RejectTemplate("user_duplicate", "重复注册",
                    "该主体已在平台注册过账号，请使用原账号登录，如需找回密码请走找回流程。"),
            new RejectTemplate("user_other", "其他原因",
                    "材料不符合平台要求，请补充完善后重新提交。")
    );

    /** 内容审核用的拒绝理由 */
    private static final List<RejectTemplate> ARTICLE_TEMPLATES = List.of(
            new RejectTemplate("article_info_incomplete", "信息不完整",
                    "发布内容的关键信息缺失（如项目预算、需求说明、上课时间等），请补充完整后重新发布。"),
            new RejectTemplate("article_inappropriate", "内容不合适",
                    "内容包含不适宜公开展示的信息，请修改后再发布。"),
            new RejectTemplate("article_contact_in_body", "正文含联系方式",
                    "为了平台安全和双方隐私，请不要在正文里直接放电话、微信等联系方式，通过平台对接功能沟通即可。"),
            new RejectTemplate("article_duplicate", "重复发布",
                    "该内容与已发布的内容重复，请勿重复发布同一份需求或项目。"),
            new RejectTemplate("article_not_real", "信息真实性存疑",
                    "内容真实性存疑，请补充可核实的说明材料后重新提交。"),
            new RejectTemplate("article_other", "其他原因",
                    "内容暂不符合平台发布规范，请修改后重新提交。")
    );

    /**
     * 拒绝理由模板列表。
     *
     * @param scene 场景：user 用户审核（默认）/ article 内容审核
     */
    @GetMapping("/reject-template/list")
    @Operation(summary = "拒绝理由模板列表")
    public Result<List<RejectTemplate>> rejectTemplateList(
            @RequestParam(value = "scene", required = false, defaultValue = "user") String scene) {
        SecurityContext.requireUserId();
        List<RejectTemplate> templates = "article".equalsIgnoreCase(scene)
                ? ARTICLE_TEMPLATES : USER_TEMPLATES;
        log.debug("查询拒绝理由模板，场景={}，共 {} 条", scene, templates.size());
        return Result.success(templates);
    }

    /** 只取模板文案，前端点一下直接把文本填进输入框 */
    @GetMapping("/reject-template/texts")
    @Operation(summary = "拒绝理由模板文案（纯文本列表）")
    public Result<List<String>> rejectTemplateTexts(
            @RequestParam(value = "scene", required = false, defaultValue = "user") String scene) {
        SecurityContext.requireUserId();
        List<RejectTemplate> templates = "article".equalsIgnoreCase(scene)
                ? ARTICLE_TEMPLATES : USER_TEMPLATES;
        List<String> texts = new ArrayList<>(templates.size());
        for (RejectTemplate template : templates) {
            texts.add(template.getContent());
        }
        return Result.success(texts);
    }

    // ==================== 下载 ====================

    /**
     * 下载文件。
     * 前端把数据库里存的 accessUrl（比如 /WebFile/article/2026/09/21/xxx.pdf）带过来，
     * 后端反推出磁盘文件再写回响应。
     */
    @GetMapping("/download-file")
    @Operation(summary = "下载文件")
    public void downloadFile(@RequestParam("url") String url,
                             @RequestParam(value = "fileName", required = false) String fileName,
                             HttpServletResponse response) throws IOException {
        SecurityContext.requireUserId();
        if (url == null || url.isBlank()) {
            throw new BusinessException("文件地址不能为空");
        }

        File file = FileDownloadUtils.resolve(url, uploadRootDir);
        String downloadName = FileDownloadUtils.fallbackName(
                fileName != null && !fileName.isBlank() ? fileName : file.getName());

        response.reset();
        response.setContentType("application/octet-stream;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition",
                FileDownloadUtils.buildContentDisposition(downloadName));
        response.setHeader("Content-Length", String.valueOf(file.length()));

        try (OutputStream out = response.getOutputStream()) {
            Files.copy(file.toPath(), out);
            out.flush();
        }
        log.info("用户 {} 下载文件 {}", SecurityContext.getUserId(), downloadName);
    }

    // ==================== 预览 ====================

    /**
     * 预览文件。
     * 图片和 PDF 走内联（inline），浏览器直接打开；其它类型浏览器也打不开，
     * 提示前端改用下载接口。
     */
    @GetMapping("/preview-file")
    @Operation(summary = "预览文件")
    public void previewFile(@RequestParam("url") String url,
                            HttpServletResponse response) throws IOException {
        SecurityContext.requireUserId();
        if (url == null || url.isBlank()) {
            throw new BusinessException("文件地址不能为空");
        }

        File file = FileDownloadUtils.resolve(url, uploadRootDir);
        String contentType = resolveContentType(file.getName());

        // 只对浏览器能直接渲染的类型做内联，剩下的让前端走下载
        boolean inline = contentType.startsWith("image/") || "application/pdf".equals(contentType);
        if (!inline) {
            throw new BusinessException("该文件类型不支持在线预览，请下载后查看");
        }

        response.reset();
        response.setContentType(contentType);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition",
                FileDownloadUtils.buildInlineDisposition(file.getName()));
        response.setHeader("Content-Length", String.valueOf(file.length()));
        // 附件按 UUID 命名，内容不会变，可以放心让浏览器缓存
        response.setHeader("Cache-Control", "max-age=86400");

        try (OutputStream out = response.getOutputStream()) {
            Files.copy(file.toPath(), out);
            out.flush();
        }
    }

    /** 按后缀猜 content-type，猜不出来就交给浏览器当二进制流 */
    private String resolveContentType(String fileName) {
        String lower = fileName == null ? "" : fileName.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".bmp")) {
            return "image/bmp";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        return "application/octet-stream";
    }

    /** 顺便提供一个把相对地址补成绝对地址的小工具，前端拼 URL 容易漏前缀 */
    @GetMapping("/build-url")
    @Operation(summary = "补全文件访问地址")
    public Result<String> buildUrl(@RequestParam("path") String path,
                                   @RequestParam(value = "bizDir", required = false) String bizDir) {
        if (path == null || path.isBlank()) {
            throw new BusinessException("文件路径不能为空");
        }
        if (path.startsWith("/")) {
            return Result.success(path);
        }
        String prefix = FileUploadUtils.buildAccessUrl(bizDir);
        return Result.success(prefix + "/" + path);
    }

    /** 拒绝理由模板结构 */
    @Data
    public static class RejectTemplate implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 模板编码，前端做 key 用 */
        private String code;

        /** 模板简称，做成可点的标签 */
        private String label;

        /** 完整文案，点一下填进意见框 */
        private String content;

        public RejectTemplate() {
        }

        public RejectTemplate(String code, String label, String content) {
            this.code = code;
            this.label = label;
            this.content = content;
        }
    }

    /** 保留这个方法名，避免和 URLEncoder 的引用一起被优化掉 */
    @SuppressWarnings("unused")
    private String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }
}
