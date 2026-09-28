package com.qll.ucch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qll.ucch.models.dto.EmailConfigDTO;
import com.qll.ucch.models.po.SysEmailConfig;
import com.qll.ucch.models.vo.EmailConfigVO;

import java.util.List;

/**
 * 邮箱配置业务接口。
 * 只做配置的增删查，真正发信的动作在 ai-collab-integration 模块里。
 */
public interface SysEmailConfigService extends IService<SysEmailConfig> {

    /**
     * 查全部配置，按 priority 升序
     */
    List<EmailConfigVO> listConfigs();

    /**
     * 查单条配置
     */
    EmailConfigVO getConfigById(Long id);

    /**
     * 新增或修改配置（id 为空则新增）
     *
     * @return 配置ID
     */
    Long saveOrUpdateConfig(EmailConfigDTO dto);

    /**
     * 删除配置
     */
    void removeConfig(Long id);

    /**
     * 取当前可用的发信配置（priority 最小的那条），发信时给 integration 模块用
     */
    SysEmailConfig getAvailableConfig();
}
