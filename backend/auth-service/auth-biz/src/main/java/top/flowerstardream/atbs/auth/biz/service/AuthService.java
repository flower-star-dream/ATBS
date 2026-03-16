package top.flowerstardream.atbs.auth.biz.service;


import top.flowerstardream.atbs.auth.ao.req.LoginRequest;
import top.flowerstardream.atbs.auth.ao.req.RefreshTokenRequest;
import top.flowerstardream.atbs.auth.ao.req.RegisterRequest;
import top.flowerstardream.atbs.auth.ao.res.LoginResponse;
import top.flowerstardream.atbs.auth.ao.res.UserInfoResponse;
import top.flowerstardream.base.result.Result;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);

    /**
     * 刷新令牌
     */
    LoginResponse refreshToken(RefreshTokenRequest request);

    /**
     * 用户注册
     */
    Result<Void> register(RegisterRequest request);

    /**
     * 用户登出
     */
    Result<Void> logout(String token);

    /**
     * 获取当前用户信息
     */
    UserInfoResponse getCurrentUser(Long userId);

    /**
     * 验证令牌
     */
    boolean validateToken(String token);
}
