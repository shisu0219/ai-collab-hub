package com.qll.ucch.openapi.blog;

import com.qll.ucch.exception.BusinessException;
import com.qll.ucch.models.PageResult;
import com.qll.ucch.models.Result;
import com.qll.ucch.models.dto.FavoriteDTO;
import com.qll.ucch.models.vo.FavoriteVO;
import com.qll.ucch.security.SecurityContext;
import com.qll.ucch.service.IUserFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 收藏 / 关注接口（新增功能）。
 * targetType=1 收藏文章，targetType=2 关注用户，前端用一个心形按钮就能搞定。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@RestController
@RequestMapping("/blog/favorite")
@RequiredArgsConstructor
@Tag(name = "收藏与关注")
public class FavoriteController {

    private final IUserFavoriteService userFavoriteService;

    @PostMapping
    @Operation(summary = "收藏 / 关注")
    public Result<Boolean> add(@Valid @RequestBody FavoriteDTO dto) {
        Long userId = SecurityContext.requireUserId();
        boolean ok = userFavoriteService.addFavorite(userId, dto.getTargetType(), dto.getTargetId());
        return Result.success(ok ? "已收藏" : "收藏失败", ok);
    }

    /**
     * 取消收藏。
     * 用 DELETE 但带上请求体，是因为取消收藏也要两个参数，放 query 里拼起来太啰嗦。
     */
    @DeleteMapping
    @Operation(summary = "取消收藏 / 取关")
    public Result<Boolean> cancel(@Valid @RequestBody FavoriteDTO dto) {
        Long userId = SecurityContext.requireUserId();
        boolean ok = userFavoriteService.cancelFavorite(userId, dto.getTargetType(), dto.getTargetId());
        return Result.success(ok ? "已取消收藏" : "取消收藏失败", ok);
    }

    @GetMapping("/list")
    @Operation(summary = "我的收藏列表")
    public Result<PageResult<FavoriteVO>> myFavorite(
            @RequestParam(value = "targetType", required = false) Integer targetType,
            @RequestParam(value = "pageNum", defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize) {
        Long userId = SecurityContext.requireUserId();
        return Result.success(PageResult.of(
                userFavoriteService.pageMyFavorite(userId, targetType, pageNum, pageSize)));
    }

    /**
     * 是否已收藏。
     * 列表页每个卡片都要问一次，所以做成轻量的单查接口，前端也可以拿 listMyFavoriteIds
     * 一次问一批，那个走 service 的批量接口。
     */
    @GetMapping("/check")
    @Operation(summary = "是否已收藏")
    public Result<Boolean> check(@RequestParam("targetType") Integer targetType,
                                 @RequestParam("targetId") Long targetId) {
        Long userId = SecurityContext.requireUserId();
        if (targetType == null || targetId == null) {
            throw new BusinessException("收藏类型和收藏对象都不能为空");
        }
        return Result.success(userFavoriteService.isFavorited(userId, targetType, targetId));
    }

    @GetMapping("/my-ids")
    @Operation(summary = "我的收藏ID集合（列表页批量回显用）")
    public Result<Map<String, Object>> myFavoriteIds(
            @RequestParam(value = "targetType", required = false, defaultValue = "1") Integer targetType) {
        Long userId = SecurityContext.requireUserId();
        return Result.success(Map.of(
                "targetType", targetType,
                "ids", userFavoriteService.listMyFavoriteIds(userId, targetType)));
    }
}
