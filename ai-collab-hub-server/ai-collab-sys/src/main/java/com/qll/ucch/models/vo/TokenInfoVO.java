package com.qll.ucch.models.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录成功后返回的 token 信息占位对象。
 * <p>
 * 真正的 token 由 ai-collab-security 模块的 JwtUtil 生成，
 * service 层不依赖 security 模块，所以这里只留字段，
 * Controller 组装返回结果时把 token 塞进来即可。
 */
@Data
@Schema(description = "登录令牌信息")
public class TokenInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "JWT 令牌，未生成时为空")
    private String token;

    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "请求头名称", example = "Authorization")
    private String headerName = "Authorization";

    @Schema(description = "过期时间（秒）", example = "86400")
    private Long expiresIn;

    public TokenInfoVO() {
    }

    public TokenInfoVO(String token, Long expiresIn) {
        this.token = token;
        this.expiresIn = expiresIn;
    }
}
