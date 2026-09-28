package com.qll.ucch.openapi.sys;

import com.qll.ucch.exception.BusinessException;
import com.qll.ucch.models.Result;
import com.qll.ucch.models.dto.EmailConfigDTO;
import com.qll.ucch.models.po.SysEmailConfig;
import com.qll.ucch.models.vo.EmailConfigVO;
import com.qll.ucch.security.SecurityContext;
import com.qll.ucch.service.SysEmailConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 运行时配置接口（管理员）。
 * 目前就一块：发信邮箱配置。平台要发审核通知邮件，SMTP 参数让管理员在页面上配，
 * 换邮箱不用改代码重新发版。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@RestController
@RequestMapping("/sys/runtime/email")
@RequiredArgsConstructor
@Tag(name = "运行时配置（管理员）")
public class SysRuntimeController {

    private final SysEmailConfigService sysEmailConfigService;

    @GetMapping("/list")
    @Operation(summary = "邮箱配置列表")
    public Result<List<EmailConfigVO>> list() {
        SecurityContext.requireUserId();
        return Result.success(sysEmailConfigService.listConfigs());
    }

    @GetMapping("/{id}")
    @Operation(summary = "邮箱配置详情")
    public Result<EmailConfigVO> detail(@PathVariable("id") Long id) {
        SecurityContext.requireUserId();
        EmailConfigVO vo = sysEmailConfigService.getConfigById(id);
        if (vo == null) {
            throw new BusinessException("配置不存在");
        }
        return Result.success(vo);
    }

    /**
     * 保存配置。
     * 有 id 就是改，没 id 就是新增，前端一个表单不用分两个接口。
     */
    @PostMapping
    @Operation(summary = "新增 / 修改邮箱配置")
    public Result<Long> save(@Valid @RequestBody EmailConfigDTO dto) {
        SecurityContext.requireUserId();
        Long id = sysEmailConfigService.saveOrUpdateConfig(dto);
        String tip = dto.getId() == null ? "配置已新增" : "配置已更新";
        log.info("管理员 {} {} 邮箱配置，id={}", SecurityContext.getUserId(), tip, id);
        return Result.success(tip, id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除邮箱配置")
    public Result<Void> remove(@PathVariable("id") Long id) {
        SecurityContext.requireUserId();
        sysEmailConfigService.removeConfig(id);
        return Result.success("配置已删除", null);
    }

    /**
     * 查看当前生效的发信配置。
     * 只回 host / 账号 / 优先级这些，密码不回给前端——虽然是管理员，
     * 也没必要让授权码在页面上来回传。
     */
    @GetMapping("/current")
    @Operation(summary = "当前生效的发信配置")
    public Result<Map<String, Object>> current() {
        SecurityContext.requireUserId();
        SysEmailConfig config = sysEmailConfigService.getAvailableConfig();
        if (config == null) {
            return Result.success("还没有可用的发信配置，请先添加一条", null);
        }

        Map<String, Object> data = new HashMap<>(8);
        data.put("id", config.getId());
        data.put("smtpHost", config.getSmtpHost());
        data.put("smtpPort", config.getSmtpPort());
        data.put("smtpUsername", config.getSmtpUsername());
        data.put("priority", config.getPriority());
        data.put("sslEnable", config.getSslEnable());
        return Result.success(data);
    }
}
