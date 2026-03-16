package top.flowerstardream.atbs.auth.biz.service.impl;

import cn.hutool.core.util.IdUtil;
import com.example.auth.dto.*;
import com.example.auth.entity.AuthClient;
import com.example.auth.entity.AuthUser;
import com.example.auth.exception.AuthException;
import com.example.auth.service.*;
import com.example.auth.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthUserService userService;
    private final AuthClientService clientService;
    private final AuthLoginLogService loginLogService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest request) {
        // 1. 验证客户端
        AuthClient client = clientService.getByClientId(request.getClientId());
        if (client == null || client.getStatus() != 1) {
            throw new AuthException("客户端无效或已禁用");
        }

        // 2. 查找用户
        AuthUser user = findUserByAccount(request.getAccount());
        if (user == null) {
            recordLoginLog(null, request.getClientId(), 1, 0, "用户不存在", null);
            throw new AuthException("账号或密码错误");
        }

        // 3. 检查用户状态
        if (user.getStatus() == 0) {
            recordLoginLog(user.getId(), request.getClientId(), 1, 0, "账户已禁用", null);
            throw new AuthException("账户已禁用");
        }

        if (user.getStatus() == 2) {
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
                recordLoginLog(user.getId(), request.getClientId(), 1, 0, "账户已锁定", null);
                throw new AuthException("账户已锁定，请稍后重试");
            } else {
                // 解锁账户
                user.setStatus(1);
                user.setLockedUntil(null);
                userService.resetFailLoginCount(user.getId());
            }
        }

        // 4. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            userService.incrementFailLoginCount(user.getId());
            int failCount = user.getFailLoginCount() + 1;
            if (failCount >= 5) {
                userService.lockAccount(user.getId(), 30);
                recordLoginLog(user.getId(), request.getClientId(), 1, 0, "密码错误，账户已锁定", null);
                throw new AuthException("密码错误次数过多，账户已锁定30分钟");
            }
            recordLoginLog(user.getId(), request.getClientId(), 1, 0, "密码错误", null);
            throw new AuthException("账号或密码错误，还剩" + (5 - failCount) + "次机会");
        }

        // 5. 登录成功，更新登录信息
        String ip = getClientIp();
        userService.updateLastLoginInfo(user.getId(), ip);
        userService.resetFailLoginCount(user.getId());
        recordLoginLog(user.getId(), request.getClientId(), 1, 1, null, null);

        // 6. 生成令牌
        List<String> roles = userService.getRoleCodesByUserId(user.getId());
        List<String> permissions = userService.getPermissionCodesByUserId(user.getId());

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roles, permissions);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpiration() / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        // 验证刷新令牌
        if (!jwtUtil.validateToken(request.getRefreshToken())) {
            throw new AuthException("刷新令牌无效或已过期");
        }

        Long userId = jwtUtil.getUserIdFromToken(request.getRefreshToken());
        AuthUser user = userService.getById(userId);

        if (user == null || user.getDeleted() == 1 || user.getStatus() != 1) {
            throw new AuthException("用户无效");
        }

        List<String> roles = userService.getRoleCodesByUserId(user.getId());
        List<String> permissions = userService.getPermissionCodesByUserId(user.getId());

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roles, permissions);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpiration() / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> register(RegisterRequest request) {
        // 1. 验证客户端
        AuthClient client = clientService.getByClientId(request.getClientId());
        if (client == null || client.getStatus() != 1) {
            throw new AuthException("客户端无效或已禁用");
        }

        // 2. 检查用户名是否已存在
        if (userService.getByUsername(request.getUsername()) != null) {
            throw new AuthException("用户名已存在");
        }

        // 3. 检查手机号是否已存在
        if (request.getPhone() != null && userService.getByPhone(request.getPhone()) != null) {
            throw new AuthException("手机号已存在");
        }

        // 4. 检查邮箱是否已存在
        if (request.getEmail() != null && userService.getByEmail(request.getEmail()) != null) {
            throw new AuthException("邮箱已存在");
        }

        // 5. 验证密码
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AuthException("两次输入的密码不一致");
        }

        // 6. 创建用户
        AuthUser user = new AuthUser();
        user.setId(IdUtil.getSnowflakeNextId());
        user.setUsername(request.getUsername());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUserType(1);
        user.setIsSuperAdmin(0);
        user.setFailLoginCount(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setCreatePersonId(0L);
        user.setUpdatePersonId(0L);
        user.setDeleted(0);
        user.setStatus(1);
        user.setVision(1);

        userService.save(user);

        return Result.success("注册成功", null);
    }

    @Override
    public Result<Void> logout(String token) {
        // 这里可以实现令牌黑名单逻辑
        // 例如将token存入Redis并设置过期时间
        return Result.success("登出成功", null);
    }

    @Override
    public UserInfoResponse getCurrentUser(Long userId) {
        AuthUser user = userService.getById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new AuthException("用户不存在");
        }

        List<String> roles = userService.getRoleCodesByUserId(userId);
        List<String> permissions = userService.getPermissionCodesByUserId(userId);

        return UserInfoResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .email(user.getEmail())
                .userType(user.getUserType())
                .isSuperAdmin(user.getIsSuperAdmin())
                .lastLoginTime(user.getLastLoginTime())
                .status(user.getStatus())
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    @Override
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    private AuthUser findUserByAccount(String account) {
        AuthUser user = userService.getByUsername(account);
        if (user != null) {
            return user;
        }

        user = userService.getByPhone(account);
        if (user != null) {
            return user;
        }

        return userService.getByEmail(account);
    }

    private void recordLoginLog(Long userId, String clientId, Integer loginType,
                                Integer loginStatus, String failReason, String userAgent) {
        try {
            String ip = getClientIp();
            loginLogService.recordLoginLog(userId, clientId, loginType, loginStatus, failReason, ip, userAgent);
        } catch (Exception e) {
            log.error("记录登录日志失败", e);
        }
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("Proxy-Client-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("WL-Proxy-Client-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            log.error("获取客户端IP失败", e);
        }
        return null;
    }
}
