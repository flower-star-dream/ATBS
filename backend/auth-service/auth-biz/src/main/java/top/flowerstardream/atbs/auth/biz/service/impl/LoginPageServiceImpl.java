package top.flowerstardream.atbs.auth.biz.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import top.flowerstardream.atbs.auth.ao.req.LoginPageREQ;
import top.flowerstardream.atbs.auth.biz.mapper.AuthUserMapper;
import top.flowerstardream.atbs.auth.biz.service.ILoginPageService;
import top.flowerstardream.atbs.auth.bo.dto.AuthorizationCodeData;
import top.flowerstardream.atbs.auth.bo.eo.AuthUserEO;
import top.flowerstardream.base.properties.OAuth2ClientProperties;
import top.flowerstardream.base.properties.OAuth2ClientProperties.ClientConfig;
import top.flowerstardream.base.state.BaseStatus;
import top.flowerstardream.base.utils.RedisUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static top.flowerstardream.atbs.auth.common.AuthExceptionEnum.*;
import static top.flowerstardream.atbs.auth.common.AuthRedisPrefixConstant.*;
import static top.flowerstardream.atbs.auth.common.OAuth2Constant.*;

/**
 * 登录页面服务实现
 * 处理后端托管登录页面的业务逻辑
 * @Author: 花海
 * @Date: 2026/03/29
 * @Description: 登录页面服务实现
 */
@Service
@Slf4j
public class LoginPageServiceImpl implements ILoginPageService {

    @Resource
    private AuthUserMapper userMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private OAuth2ClientProperties oAuth2ClientProperties;

    @Resource
    private RedisUtils redisUtils;

    /**
     * 处理登录请求
     * 验证用户凭据，生成授权码，并重定向到回调地址
     *
     * @param req      登录请求参数
     * @param response HTTP响应对象
     * @throws IOException 重定向时可能抛出IO异常
     */
    @Override
    public void handleLogin(LoginPageREQ req, HttpServletResponse response) throws IOException {
        // 1. 参数校验
        if (!StringUtils.hasText(req.getUsername()) || !StringUtils.hasText(req.getPassword())) {
            redirectToLoginWithError(response, req, INVALID_REQUEST.getCode().toString(),
                    "用户名或密码不能为空");
            return;
        }

        // 2. 验证客户端
        ClientConfig client;
        try {
            client = validateClient(req.getClientId());
        } catch (Exception e) {
            redirectToLoginWithError(response, req, INVALID_CLIENT.getCode().toString(),
                    "无效的客户端");
            return;
        }

        // 3. 验证重定向URI
        if (!isValidRedirectUri(req.getRedirectUri(), client.getRedirectUris())) {
            redirectToLoginWithError(response, req, INVALID_REDIRECT_URI.getCode().toString(),
                    "无效的重定向地址");
            return;
        }

        // 4. 验证用户凭据（支持用户名/手机号/邮箱）
        AuthUserEO user = authenticateUser(req.getUsername(), req.getPassword());
        if (user == null) {
            redirectToLoginWithError(response, req, ACCESS_DENIED.getCode().toString(),
                    "用户名或密码错误");
            return;
        }

        // 5. 检查用户状态
        if (user.getStatus() == BaseStatus.DISABLE) {
            redirectToLoginWithError(response, req, ACCESS_DENIED.getCode().toString(),
                    "用户账号已被禁用");
            return;
        }

        // 6. 验证scope
        String finalScope;
        try {
            finalScope = validateScope(req.getScope(), client.getScopes());
        } catch (Exception e) {
            redirectToLoginWithError(response, req, INVALID_SCOPE.getCode().toString(),
                    "无效的权限范围");
            return;
        }

        // 7. 生成授权码
        String authCode = generateAuthorizationCode();
        AuthorizationCodeData codeData = AuthorizationCodeData.builder()
                .code(authCode)
                .userId(user.getId())
                .clientId(req.getClientId())
                .redirectUri(req.getRedirectUri())
                .scope(finalScope)
                .codeChallenge(req.getCodeChallenge())
                .codeChallengeMethod(req.getCodeChallengeMethod())
                .createTime(LocalDateTime.now())
                .build();

        // 8. 存储授权码到Redis（5分钟有效期）
        boolean saved = redisUtils.set(
                OAUTH2_CODE_PREFIX + authCode,
                JSON.toJSONString(codeData),
                AUTH_CODE_TTL_MINUTES,
                TimeUnit.MINUTES
        );
        if (!saved) {
            redirectToLoginWithError(response, req, SERVER_ERROR.getCode().toString(),
                    "系统繁忙，请稍后重试");
            return;
        }

        log.info("用户登录成功 - userId: {}, username: {}, clientId: {}",
                user.getId(), user.getUsername(), req.getClientId());

        // 9. 重定向到回调地址，携带授权码
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(req.getRedirectUri())
                .queryParam(CODE, authCode);

        if (StringUtils.hasText(req.getState())) {
            builder.queryParam(STATE, req.getState());
        }

        response.sendRedirect(builder.build().toUriString());
    }

    /**
     * 验证客户端
     *
     * @param clientId 客户端ID
     * @return 客户端配置
     */
    private ClientConfig validateClient(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            throw INVALID_CLIENT.toException();
        }
        return oAuth2ClientProperties.getClient(clientId)
                .orElseThrow(INVALID_CLIENT::toException);
    }

    /**
     * 验证重定向URI
     *
     * @param redirectUri    请求的重定向URI
     * @param registeredUris 注册的重定向URI列表
     * @return 是否有效
     */
    private boolean isValidRedirectUri(String redirectUri, List<String> registeredUris) {
        log.info("验证重定向URI - redirectUri: {}, registeredUris: {}", redirectUri, registeredUris);
        return StringUtils.hasText(redirectUri) &&
               registeredUris != null &&
               registeredUris.contains(redirectUri);
    }

    /**
     * 验证用户凭据
     * 支持用户名、手机号、邮箱登录
     *
     * @param username 用户名/手机号/邮箱
     * @param password 密码
     * @return 用户信息，验证失败返回null
     */
    private AuthUserEO authenticateUser(String username, String password) {
        log.info("验证用户凭据 - username: {}, password: {}", username, password);
        // 构建查询条件，支持用户名/手机号/邮箱
        LambdaQueryWrapper<AuthUserEO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuthUserEO::getUsername, username)
                .or()
                .eq(AuthUserEO::getPhone, username)
                .or()
                .eq(AuthUserEO::getEmail, username);

        AuthUserEO user = userMapper.selectOne(wrapper);

        // 验证密码
        log.info("验证密码 - password: {}, dpassword: {}", password, user.getPassword());
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return null;
        }

        return user;
    }

    /**
     * 验证权限范围
     *
     * @param requested 请求的scope
     * @param allowed   允许的scope列表
     * @return 最终scope
     */
    private String validateScope(String requested, List<String> allowed) {
        if (!StringUtils.hasText(requested)) {
            return String.join(" ", allowed);
        }

        Set<String> reqSet = Set.of(requested.split(" "));
        Set<String> allowSet = new HashSet<>(allowed);

        if (!allowSet.containsAll(reqSet)) {
            throw INVALID_SCOPE.toException();
        }

        return requested;
    }

    /**
     * 生成授权码
     *
     * @return Base64编码的UUID
     */
    private String generateAuthorizationCode() {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(UUID.randomUUID().toString().getBytes());
    }

    /**
     * 登录失败时重定向回登录页，保留所有OAuth2参数
     *
     * @param response HTTP响应对象
     * @param req      登录请求参数
     * @param error    错误码
     * @param description 错误描述
     * @throws IOException 重定向时可能抛出IO异常
     */
    private void redirectToLoginWithError(HttpServletResponse response, LoginPageREQ req,
                                          String error, String description) throws IOException {
        log.warn("登录失败 - error: {}, description: {}", error, description);

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/login")
                .queryParam("error", error)
                .queryParam("error_description", description);

        // 保留所有OAuth2参数，以便用户可以重新登录
        if (StringUtils.hasText(req.getClientId())) {
            builder.queryParam(CLIENT_ID, req.getClientId());
        }
        if (StringUtils.hasText(req.getRedirectUri())) {
            builder.queryParam(REDIRECT_URI, req.getRedirectUri());
        }
        if (StringUtils.hasText(req.getScope())) {
            builder.queryParam(SCOPE, req.getScope());
        }
        if (StringUtils.hasText(req.getState())) {
            builder.queryParam(STATE, req.getState());
        }
        if (StringUtils.hasText(req.getCodeChallenge())) {
            builder.queryParam(CODE_CHALLENGE, req.getCodeChallenge());
        }
        if (StringUtils.hasText(req.getCodeChallengeMethod())) {
            builder.queryParam(CODE_CHALLENGE_METHOD, req.getCodeChallengeMethod());
        }

        response.sendRedirect(builder.build().encode().toUriString());
    }
}
