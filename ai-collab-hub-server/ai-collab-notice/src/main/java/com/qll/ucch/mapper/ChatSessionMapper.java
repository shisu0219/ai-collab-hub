package com.qll.ucch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qll.ucch.models.po.ChatSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 站内会话 Mapper（新增）。
 *
 * @author qll
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
