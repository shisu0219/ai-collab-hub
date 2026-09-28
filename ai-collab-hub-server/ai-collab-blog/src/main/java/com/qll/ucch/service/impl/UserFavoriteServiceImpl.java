package com.qll.ucch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qll.ucch.mapper.UserFavoriteMapper;
import com.qll.ucch.models.common.BusinessException;
import com.qll.ucch.models.enums.FavoriteTypeEnum;
import com.qll.ucch.models.po.UserFavorite;
import com.qll.ucch.models.vo.FavoriteVO;
import com.qll.ucch.service.IUserFavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 收藏/关注实现（新增功能）。
 * 不需要文章详情的地方就只查 user_favorite 一张表，文章的展示字段交给上层补，
 * 这样本模块不必反向依赖别的模块的 Service。
 *
 * @author qll
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserFavoriteServiceImpl implements IUserFavoriteService {

    private final UserFavoriteMapper userFavoriteMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addFavorite(Long userId, Integer targetType, Long targetId) {
        checkParam(userId, targetType, targetId);

        // 幂等：已经收藏过就直接返回成功，前端可以放心地重复点
        if (isFavorited(userId, targetType, targetId)) {
            return true;
        }

        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setTargetType(targetType);
        favorite.setTargetId(targetId);
        userFavoriteMapper.insert(favorite);
        log.info("用户 {} 收藏/关注成功，targetType={}, targetId={}", userId, targetType, targetId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelFavorite(Long userId, Integer targetType, Long targetId) {
        checkParam(userId, targetType, targetId);

        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getTargetType, targetType)
                .eq(UserFavorite::getTargetId, targetId);
        // 表上没有逻辑删除字段，这里直接物理删，重复调用也无所谓
        int rows = userFavoriteMapper.delete(wrapper);
        log.info("用户 {} 取消收藏，targetType={}, targetId={}, 影响行数={}", userId, targetType, targetId, rows);
        return true;
    }

    @Override
    public boolean isFavorited(Long userId, Integer targetType, Long targetId) {
        if (userId == null || targetType == null || targetId == null) {
            // 未登录 / 参数不全一律当未收藏处理，避免列表页报错
            return false;
        }
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getTargetType, targetType)
                .eq(UserFavorite::getTargetId, targetId);
        return userFavoriteMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Page<FavoriteVO> pageMyFavorite(Long userId, Integer targetType, Long pageNum, Long pageSize) {
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        long current = pageNum == null || pageNum < 1 ? 1L : pageNum;
        long size = pageSize == null || pageSize < 1 ? 10L : pageSize;

        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getUserId, userId)
                .eq(targetType != null, UserFavorite::getTargetType, targetType)
                .orderByDesc(UserFavorite::getCreateTime);

        Page<UserFavorite> poPage = userFavoriteMapper.selectPage(new Page<>(current, size), wrapper);

        Page<FavoriteVO> voPage = new Page<>(current, size, poPage.getTotal());
        List<FavoriteVO> records = poPage.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public List<Long> listMyFavoriteIds(Long userId, Integer targetType) {
        if (userId == null || targetType == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getTargetType, targetType)
                .select(UserFavorite::getTargetId);
        return userFavoriteMapper.selectList(wrapper).stream()
                .map(UserFavorite::getTargetId)
                .collect(Collectors.toList());
    }

    @Override
    public Long countByTarget(Integer targetType, Long targetId) {
        if (targetType == null || targetId == null) {
            return 0L;
        }
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getTargetType, targetType)
                .eq(UserFavorite::getTargetId, targetId);
        return userFavoriteMapper.selectCount(wrapper);
    }

    /**
     * 基础参数校验，顺手把非法的 targetType 挡在门外。
     */
    private void checkParam(Long userId, Integer targetType, Long targetId) {
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        if (targetId == null) {
            throw new BusinessException("收藏对象不能为空");
        }
        if (FavoriteTypeEnum.of(targetType) == null) {
            throw new BusinessException("收藏类型不合法");
        }
    }

    /**
     * PO -> VO。文章的标题、类型这类信息这里不查，由 admin 层的聚合逻辑补上，
     * 这里先把类型名称填了，前端至少能知道这条收藏的是文章还是人。
     */
    private FavoriteVO toVO(UserFavorite po) {
        FavoriteVO vo = new FavoriteVO();
        vo.setId(po.getId());
        vo.setTargetType(po.getTargetType());
        vo.setTargetId(po.getTargetId());
        vo.setCreateTime(po.getCreateTime());

        FavoriteTypeEnum typeEnum = FavoriteTypeEnum.of(po.getTargetType());
        vo.setTargetTypeName(typeEnum == null ? null : typeEnum.getName());
        return vo;
    }
}
