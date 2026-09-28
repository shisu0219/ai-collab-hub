package com.qll.ucch.openapi.blog;

import java.util.HashMap;
import java.util.Map;
import com.qll.ucch.models.vo.UserInfoVO;
import com.qll.ucch.constance.FileConst;
import com.qll.ucch.exception.BusinessException;
import com.qll.ucch.models.PageResult;
import com.qll.ucch.models.Result;
import com.qll.ucch.models.dto.ArticleQueryDTO;
import com.qll.ucch.models.dto.ArticleSaveDTO;
import com.qll.ucch.models.po.BlogArticleStatus;
import com.qll.ucch.models.po.BlogArticleTag;
import com.qll.ucch.models.po.BlogProgress;
import com.qll.ucch.models.po.BlogType;
import com.qll.ucch.models.po.SysUser;
import com.qll.ucch.models.vo.ArticleDetailVO;
import com.qll.ucch.models.vo.ArticleListVO;
import com.qll.ucch.models.vo.DictVO;
import com.qll.ucch.models.vo.FileInfoVO;
import com.qll.ucch.security.IgnoreAuth;
import com.qll.ucch.security.SecurityContext;
import com.qll.ucch.service.IBlogArticleService;
import com.qll.ucch.service.IBlogDictService;
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

import java.util.List;

/**
 * 合作内容（文章）接口。
 * 项目 / 需求 / 课程三类内容共用一套接口，靠 typeId 区分，前端只换表单。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@RestController
@RequestMapping("/blog/article")
@RequiredArgsConstructor
@Tag(name = "合作内容")
public class BlogArticleController {

    private final IBlogArticleService blogArticleService;

    private final IBlogDictService blogDictService;

    /** 补发布者昵称和角色用（blog 模块查不到 sys_user） */
    private final SysUserService sysUserService;

    @Value("${ai-collab.upload.root-dir:}")
    private String uploadRootDir;

    // ==================== 发布 / 修改 / 删除 ====================

    @PostMapping
    @Operation(summary = "发布内容")
    public Result<Long> publish(@Valid @RequestBody ArticleSaveDTO dto) {
        Long userId = SecurityContext.requireUserId();
        // 发布者以登录态为准，不信任前端传的 userId
        dto.setUserId(userId);
        Long articleId = blogArticleService.publishArticle(dto, userId);
        return Result.success("发布成功，等待管理员审核", articleId);
    }

    @PutMapping
    @Operation(summary = "修改内容")
    public Result<Void> update(@Valid @RequestBody ArticleSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("请指定要修改的内容");
        }
        Long userId = SecurityContext.requireUserId();
        dto.setUserId(userId);

        // 管理员改内容不校验归属，也不会把状态打回待审
        if (SecurityContext.isAdmin()) {
            blogArticleService.updateArticleByAdmin(dto);
        } else {
            blogArticleService.updateArticle(dto, userId);
        }
        return Result.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除内容")
    public Result<Void> delete(@PathVariable("id") Long id) {
        blogArticleService.deleteArticle(id, SecurityContext.requireUserId());
        return Result.success("已删除", null);
    }

    @DeleteMapping("/attachment")
    @Operation(summary = "删除内容附件")
    public Result<Void> deleteAttachment(@RequestParam("articleId") Long articleId,
                                         @RequestParam("attachment") String attachment) {
        blogArticleService.deleteAttachment(articleId, attachment, SecurityContext.requireUserId());
        return Result.success("附件已移除", null);
    }

    /**
     * 上传附件。
     * 只负责把文件存下来并回地址，落库由发布 / 修改接口的 attachments 字段完成，
     * 这样编辑页可以先传完再一起提交。
     */
    @PostMapping("/upload")
    @Operation(summary = "上传内容附件")
    public Result<FileInfoVO> uploadAttachment(@RequestParam("file") MultipartFile file) {
        SecurityContext.requireUserId();
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        FileInfoVO fileInfo = FileUploadUtils.upload(file, uploadRootDir, FileConst.DIR_ARTICLE);
        return Result.success("上传成功", fileInfo);
    }

    // ==================== 静态字典 ====================

    @IgnoreAuth
    @GetMapping("/static/type")
    @Operation(summary = "内容类型字典")
    public Result<List<BlogType>> typeDict() {
        return Result.success(blogDictService.listType());
    }

    @IgnoreAuth
    @GetMapping("/static/status")
    @Operation(summary = "内容状态字典")
    public Result<List<BlogArticleStatus>> statusDict() {
        return Result.success(blogDictService.listStatus());
    }

    @IgnoreAuth
    @GetMapping("/static/progress")
    @Operation(summary = "内容进度字典")
    public Result<List<BlogProgress>> progressDict(
            @RequestParam(value = "typeId", required = false) Long typeId) {
        return Result.success(blogDictService.listProgress(typeId));
    }

    @IgnoreAuth
    @GetMapping("/static/tag")
    @Operation(summary = "标签字典")
    public Result<List<BlogArticleTag>> tagDict(
            @RequestParam(value = "tagName", required = false) String tagName) {
        return Result.success(blogDictService.listTag(tagName));
    }

    @IgnoreAuth
    @GetMapping("/static/all")
    @Operation(summary = "全部字典（一次拿全，前端少发请求）")
    public Result<DictVO> allDict(@RequestParam(value = "typeId", required = false) Long typeId) {
        return Result.success(blogDictService.getAllDict(typeId));
    }

    // ==================== 列表 / 详情 ====================

    @IgnoreAuth
    @GetMapping("/list/brief")
    @Operation(summary = "内容列表（公开，未登录也能看）")
    public Result<PageResult<ArticleListVO>> briefList(ArticleQueryDTO query) {
        // 公开列表只放已发布的内容，把待审/被拒的挡掉
        query.setStatusId(2L);
        PageResult<ArticleListVO> page = PageResult.of(blogArticleService.pageArticleList(query));
        fillListPublishers(page.getRecords());
        return Result.success(page);
    }

    @GetMapping("/list")
    @Operation(summary = "我的发布")
    public Result<PageResult<ArticleListVO>> myList(ArticleQueryDTO query) {
        Long userId = SecurityContext.requireUserId();
        query.setUserId(userId);
        PageResult<ArticleListVO> page = PageResult.of(blogArticleService.pageMyArticle(query, userId));
        fillListPublishers(page.getRecords());
        return Result.success(page);
    }

    @GetMapping("/list/by-user")
    @Operation(summary = "某用户发布的内容（个人主页）")
    public Result<PageResult<ArticleListVO>> listByUser(ArticleQueryDTO query) {
        if (query.getUserId() == null) {
            throw new BusinessException("请指定要查看的用户");
        }
        PageResult<ArticleListVO> page = PageResult.of(blogArticleService.pageArticleByUser(query));
        fillListPublishers(page.getRecords());
        return Result.success(page);
    }

    /**
     * 批量为列表回填发布者昵称和角色。
     *
     * 【为什么要批量】
     * 列表一页 10~20 条，如果每条都去查一次用户表和角色表，就是典型的 N+1，
     * 数据一多接口就慢得明显。这里先收集 ID 一次查完再回填。
     *
     * 【为什么列表需要角色】
     * 前端要在列表上标「老师发布」—— 学生得能一眼看出这条是老师的课题还是同学的项目。
     * 这个字段后端不给，前端就只能瞎猜（本项目踩过「前端凭空发明角色字段」的坑）。
     */
    private void fillListPublishers(List<ArticleListVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> userIds = list.stream()
                .map(ArticleListVO::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        if (userIds.isEmpty()) {
            return;
        }
        try {
            Map<Long, SysUser> userMap = new HashMap<>();
            List<SysUser> users = sysUserService.listByIds(userIds);
            if (users != null) {
                for (SysUser u : users) {
                    userMap.put(u.getId(), u);
                }
            }
            for (ArticleListVO vo : list) {
                SysUser u = userMap.get(vo.getUserId());
                if (u == null) {
                    continue;
                }
                vo.setPublisherName(cn.hutool.core.util.StrUtil.isNotBlank(u.getNickname())
                        ? u.getNickname() : u.getAccount());
                // 角色：逐个人查一次角色表（人数已被 distinct 压过，且一页最多几十个）
                UserInfoVO info = sysUserService.getUserInfoById(u.getId());
                if (info != null && info.getRoleCodes() != null && !info.getRoleCodes().isEmpty()) {
                    vo.setPublisherRoleCode(info.getRoleCodes().get(0));
                    if (info.getRoleNames() != null && !info.getRoleNames().isEmpty()) {
                        vo.setPublisherRoleName(info.getRoleNames().get(0));
                    }
                }
            }
        } catch (Exception e) {
            // 回填失败不能让列表挂掉，顶多少几个名字和标识
            log.warn("回填列表发布者信息失败：{}", e.getMessage());
        }
    }

    @IgnoreAuth
    @GetMapping("/list/brief/{id}")
    @Operation(summary = "内容详情")
    public Result<ArticleDetailVO> detail(@PathVariable("id") Long id) {
        // 未登录时为 null，登录了就带上 userId（详情里要标「我是否收藏过」）
        ArticleDetailVO vo = blogArticleService.getArticleDetail(id, SecurityContext.getUserId());
        if (vo == null) {
            throw new BusinessException("内容不存在或已被删除");
        }
        fillPublisher(vo);
        return Result.success(vo);
    }

    /**
     * 把发布者的昵称和角色补到详情上。
     * <p>
     * 以前只靠一个布尔值判断「是不是老师发布」，判断不准，
     * 前端显示的发布者角色就一直是错的，所以在 Controller 层直接补准确的角色码。
     */
    private void fillPublisher(ArticleDetailVO vo) {
        if (vo == null || vo.getUserId() == null) {
            return;
        }
        SysUser user = sysUserService.getById(vo.getUserId());
        if (user == null) {
            return;
        }
        if (cn.hutool.core.util.StrUtil.isNotBlank(user.getNickname())) {
            vo.setPublisherName(user.getNickname());
        } else {
            vo.setPublisherName(user.getAccount());
        }
        vo.setPublisherAvatar(user.getAvatar());

        // 用 getUserInfoById 拿角色（返回的 VO 里带 roleCodes / roleNames）
        com.qll.ucch.models.vo.UserInfoVO info = sysUserService.getUserInfoById(user.getId());
        if (info != null && info.getRoleCodes() != null && !info.getRoleCodes().isEmpty()) {
            String code = info.getRoleCodes().get(0);
            vo.setPublisherRoleCode(code);
            if (info.getRoleNames() != null && !info.getRoleNames().isEmpty()) {
                vo.setPublisherRoleName(info.getRoleNames().get(0));
            }
        }
    }
}
