package top.flowerstardream.atbs.auth.api.v1.mgmt;


import com.alibaba.nacos.api.model.v2.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.auth.ao.req.LoginRequest;
import top.flowerstardream.atbs.auth.ao.req.RefreshTokenRequest;
import top.flowerstardream.atbs.auth.ao.req.RegisterRequest;
import top.flowerstardream.atbs.auth.ao.res.LoginResponse;
import top.flowerstardream.atbs.auth.ao.res.UserInfoResponse;
import top.flowerstardream.atbs.auth.biz.service.AuthService;
import top.flowerstardream.base.utils.JwtUtil;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private AuthService authService;
    private JwtUtil jwtUtil;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success();
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    public Result<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request);
        return Result.success();
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return authService.logout(token);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/userinfo")
    public Result<UserInfoResponse> getUserInfo(@RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        UserInfoResponse response = authService.getCurrentUser(userId);
        return Result.success(response);
    }

    /**
     * 验证令牌
     */
    @GetMapping("/validate")
    public Result<Boolean> validateToken(@RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        boolean valid = authService.validateToken(token);
        return Result.success(valid);
    }
}
