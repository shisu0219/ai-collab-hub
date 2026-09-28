package com.qll.ucch.openapi.user;

import com.qll.ucch.constance.redis.RedisKeyConst;
import com.qll.ucch.models.Result;
import com.qll.ucch.models.dto.ResetByPhoneDTO;
import com.qll.ucch.models.dto.ResetPasswordDTO;
import com.qll.ucch.models.dto.StudentLoginDTO;
import com.qll.ucch.models.dto.TeacherRegisterDTO;
import com.qll.ucch.models.dto.StudentRegisterDTO;
import com.qll.ucch.models.vo.LoginVO;
import com.qll.ucch.models.vo.TokenInfoVO;
import com.qll.ucch.security.IgnoreAuth;
import com.qll.ucch.security.JwtUtil;
import com.qll.ucch.security.SecurityContext;
import com.qll.ucch.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 登录认证接口。
 * 学生端、老师端分开登录，注册也分开，因为字段校验要求不一样。
 * <p>
 * token 由 security 模块的 JwtUtil 签发，登录成功后往 Redis 里塞一份登录态，
 * AuthInterceptor 每次请求都会拿 token 去 Redis 对一下，这样管理员踢人下线才生效。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "认证与登录")
public class AuthController {

    private final SysUserService sysUserService;

    private final JwtUtil jwtUtil;

    private final StringRedisTemplate redisTemplate;

    // ==================== 登录 ====================

    @IgnoreAuth
    @PostMapping("/login/student")
    @Operation(summary = "学生登录")
    public Result<LoginVO> loginStudent(@Valid @RequestBody StudentLoginDTO dto) {
        LoginVO loginVO = sysUserService.loginByStudent(dto);
        return Result.success("登录成功", fillToken(loginVO));
    }

    @IgnoreAuth
    @PostMapping("/login/admin")
    @Operation(summary = "管理员登录")
    public Result<LoginVO> loginAdmin(@Valid @RequestBody StudentLoginDTO dto) {
        LoginVO loginVO = sysUserService.loginByAdmin(dto);
        return Result.success("登录成功", fillToken(loginVO));
    }

    @IgnoreAuth
    @PostMapping("/login/teacher")
    @Operation(summary = "老师登录")
    public Result<LoginVO> loginTeacher(@Valid @RequestBody StudentLoginDTO dto) {
        LoginVO loginVO = sysUserService.loginByTeacher(dto);
        return Result.success("登录成功", fillToken(loginVO));
    }

    // ==================== 注册 ====================

    @IgnoreAuth
    @PostMapping("/register/common")
    @Operation(summary = "学生注册")
    public Result<Long> registerStudent(@Valid @RequestBody StudentRegisterDTO dto) {
        Long userId = sysUserService.registerStudent(dto);
        return Result.success("注册已提交，请等待管理员审核", userId);
    }

    @IgnoreAuth
    @PostMapping("/register/teacher")
    @Operation(summary = "老师注册")
    public Result<Long> registerTeacher(@Valid @RequestBody TeacherRegisterDTO dto) {
        Long userId = sysUserService.registerTeacher(dto);
        return Result.success("注册已提交，请等待管理员审核", userId);
    }

    // ==================== 找回密码 ====================
    //
    // 两种方式并行，任选其一都能重置密码：
    //   邮箱版：账号 + 邮箱      —— 邮箱是注册时填的（选填，没填的账号用不了这种）
    //   手机号版：账号 + 手机号   —— 手机号注册必填，人人都有（前端默认走这个）
    //
    // 两种都保留的理由：用户可能只记得其中一个；各自独立，互不影响。

    /**
     * 找回密码 —— 邮箱版。
     *
     * 校验「账号 + 邮箱」，两个都对上才允许重置。
     * 注：邮箱在注册时是选填的，没填邮箱的账号要走手机号版。
     */
    @IgnoreAuth
    @PostMapping("/find/pwd/email")
    @Operation(summary = "重置密码（账号 + 邮箱双重校验）")
    public Result<Void> findPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        sysUserService.resetPassword(dto);
        return Result.success("密码已重置，请用新密码登录", null);
    }

    /**
     * 找回密码 —— 手机号版（前端现在走这个）。
     *
     * 【为什么前端选它】手机随身带、不容易忘，而且 phone 列有唯一索引能唯一定位到人。
     * 手机号在注册时是必填的，所以每个账号都能用这个方式找回。
     */
    @IgnoreAuth
    @PostMapping("/find/pwd/phone")
    @Operation(summary = "重置密码（账号 + 手机号双重校验）")
    public Result<Void> findPasswordByPhone(@Valid @RequestBody ResetByPhoneDTO dto) {
        sysUserService.resetPasswordByPhone(dto);
        return Result.success("密码已重置，请用新密码登录", null);
    }

    // ==================== 退出 ====================

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Result<Void> logout() {
        Long userId = SecurityContext.getUserId();
        if (userId != null) {
            // 把登录态删掉，这个 token 立刻就不管用了
            redisTemplate.delete(RedisKeyConst.LOGIN_USER_PREFIX + userId);
        }
        return Result.success("已退出登录", null);
    }

    // ==================== 私有方法 ====================

    /**
     * 给登录结果补上真实 token，并把登录态写进 Redis。
     * service 层不依赖 security 模块，所以 token 的生成只能放在 Controller 里做。
     */
    private LoginVO fillToken(LoginVO loginVO) {
        if (loginVO == null || loginVO.getUserId() == null) {
            return loginVO;
        }

        // 角色取第一个，JWT 里只带一个角色标识就够了，完整角色列表在 roleCodes 里
        String roleCode = (loginVO.getRoleCodes() == null || loginVO.getRoleCodes().isEmpty())
                ? null : loginVO.getRoleCodes().get(0);

        String token = jwtUtil.createToken(loginVO.getUserId(), loginVO.getAccount(),
                null, roleCode);

        TokenInfoVO tokenInfo = new TokenInfoVO(token, RedisKeyConst.TOKEN_EXPIRE_SECONDS);
        loginVO.setTokenInfo(tokenInfo);

        // 登录态：key 存在就代表没被踢，value 存个角色方便排查
        Map<String, String> loginState = new HashMap<>(4);
        loginState.put("account", loginVO.getAccount());
        loginState.put("roleCode", roleCode);
        loginState.put("loginTime", String.valueOf(System.currentTimeMillis()));
        redisTemplate.opsForHash().putAll(RedisKeyConst.LOGIN_USER_PREFIX + loginVO.getUserId(), loginState);
        redisTemplate.expire(RedisKeyConst.LOGIN_USER_PREFIX + loginVO.getUserId(),
                RedisKeyConst.TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);

        log.info("用户 {} 登录成功，角色 {}", loginVO.getAccount(), roleCode);
        return loginVO;
    }
}
