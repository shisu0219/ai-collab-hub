package com.qll.ucch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qll.ucch.models.po.SysUserReview;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户审核记录表 Mapper
 */
@Mapper
public interface SysUserReviewMapper extends BaseMapper<SysUserReview> {
}
