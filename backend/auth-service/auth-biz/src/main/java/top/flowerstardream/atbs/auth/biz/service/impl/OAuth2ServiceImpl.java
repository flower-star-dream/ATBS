package top.flowerstardream.atbs.auth.biz.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import top.flowerstardream.atbs.auth.ao.req.OAuth2AuthorizeREQ;
import top.flowerstardream.atbs.auth.ao.req.OAuth2TokenREQ;
import top.flowerstardream.atbs.auth.ao.req.UserInfoREQ;
import top.flowerstardream.atbs.auth.ao.req.WechatLoginREQ;
import top.flowerstardream.atbs.auth.ao.res.*;
import top.flowerstardream.atbs.auth.biz.mapper.AuthRoleMapper;
import top.flowerstardream.atbs.auth.biz.mapper.AuthUserMapper;
import top.flowerstardream.atbs.auth.biz.mapper.AuthUserRoleMapper;
import top.flowerstardream.atbs.auth.biz.mapper.AuthUserSocialMapper;
import top.flowerstardream.atbs.auth.biz.service.IAuthRoleService;
import top.flowerstardream.atbs.auth.biz.service.IAuthUserService;
import top.flowerstardream.atbs.auth.biz.service.IOAuth2Service;
import top.flowerstardream.atbs.auth.bo.dto.AuthorizationCodeData;
import top.flowerstardream.atbs.auth.bo.eo.AuthUserEO;
import top.flowerstardream.atbs.auth.bo.eo.RoleEO;
import top.flowerstardream.atbs.auth.bo.eo.UserRoleEO;
import top.flowerstardream.atbs.auth.common.AuthRedisPrefixConstant;
import top.flowerstardream.atbs.tools.constants.ClientType;
import top.flowerstardream.atbs.tools.constants.JwtClaimsConstant;
import top.flowerstardream.base.properties.JwtProperties;
import top.flowerstardream.base.properties.JwtProperties.TokenConfig;
import top.flowerstardream.base.properties.OAuth2ClientProperties;
import top.flowerstardream.base.properties.OAuth2ClientProperties.ClientConfig;
import top.flowerstardream.base.properties.BusSystemProperties;
import top.flowerstardream.base.service.Impl.BaseServiceImpl;
import top.flowerstardream.base.service.VerificationCodeService;
import top.flowerstardream.base.state.BaseStatus;
import top.flowerstardream.base.utils.JwtUtil;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static top.flowerstardream.atbs.auth.common.AuthExceptionEnum.*;
import static top.flowerstardream.atbs.auth.common.AuthRedisPrefixConstant.*;
import static top.flowerstardream.atbs.auth.common.WxConstant.*;
import static top.flowerstardream.atbs.tools.constants.CommonConstant.*;
import static top.flowerstardream.atbs.tools.constants.JwtClaimsConstant.*;
import static top.flowerstardream.atbs.auth.common.OAuth2Constant.*;
import static top.flowerstardream.atbs.tools.constants.JwtClaimsConstant.CLIENT_ID;
import static top.flowerstardream.atbs.tools.constants.JwtClaimsConstant.OPERATOR_ID;
import static top.flowerstardream.atbs.tools.constants.JwtClaimsConstant.OPERATOR_NAME;
import static top.flowerstardream.atbs.tools.constants.JwtClaimsConstant.SCOPE;
import static top.flowerstardream.base.constant.CommonConstant.*;
import static top.flowerstardream.base.exception.BaseExceptionEnum.*;
import static top.flowerstardream.base.exception.ExceptionEnum.*;
import static top.flowerstardream.base.utils.GetInfoUtil.*;

/**
 * @Author: 花海
 * @Date: 2026/03/17/15:44
 * @Description: OAuth2服务实现
 */
@Service
@Slf4j
public class OAuth2ServiceImpl implements IOAuth2Service {

    @Resource
    private OAuth2ClientProperties oAuth2ClientProperties;

    @Resource
    private BusSystemProperties busSystemProperties;

    @Resource
    private AuthUserMapper userMapper;

    @Resource
    private AuthRoleMapper roleMapper;

    @Resource
    private AuthUserRoleMapper userRoleMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private JwtProperties jwtProperties;

    @Resource
    private IAuthUserService authUserService;

    @Resource
    private IAuthRoleService authRoleService;

    @Resource
    @Lazy
    private IOAuth2Service self;

    /**
     * 授权端点
     * @param req
     * @param response
     */
    @Override
    public void authorize(OAuth2AuthorizeREQ req, HttpServletResponse response) throws IOException {
        // 验证 response_type
        if (!CODE_RES_TYPE.equals(req.getResponseType())) {
            throw UNSUPPORTED_RESPONSE_TYPE.toException();
        }

        Long userId = getOperatorId();
        if (userId == null) {
            throw UNAUTHORIZED.toException();
        }

        handleCodeFlow(userId, req.getClientId(), req.getRedirectUri(), req.getScope(), req.getState(), null, null, response);
    }

    /**
     * 令牌撤销
     *
     * @param token
     * @param tokenTypeHint
     */
    @Override
    public void revokeToken(String token, String tokenTypeHint) {
        if (!StringUtils.hasText(token)) {
            return; // RFC 7009: 无效token也返回200
        }
        ClientType clientType = getExtra(CLIENT_TYPE, ClientType.class);
        TokenConfig tokenConfig = jwtProperties.getTokens().get(clientType.getName());

        // 解析获取过期时间
        Claims claims = JwtUtil.getClaimsBody(tokenConfig.getSecretKey(), token);
        if (claims == null) {
            log.warn("令牌撤销失败：无效的令牌");
            return;
        }
        int val = JwtUtil.verifyToken(claims, tokenConfig.getRefreshTime());
        if (val <= 0) {
            // 加入黑名单，TTL 为令牌剩余有效期
            stringRedisTemplate.opsForValue().set(OAUTH2_BLACKLIST_PREFIX + token, "revoked", val, TimeUnit.SECONDS
            );
        }
    }

    /**
     * 令牌内省
     *
     * @param token
     * @param tokenTypeHint
     * @return
     */
    @Override
    public IntrospectRES introspectToken(String token, String tokenTypeHint) {
        IntrospectRES response = new IntrospectRES();

        if (!StringUtils.hasText(token)) {
            response.setActive(false);
            return response;
        }

        // 检查黑名单
        if (isTokenBlacklisted(token)) {
            response.setActive(false);
            return response;
        }

        // 解析token
        ClientType clientType = getExtra(CLIENT_TYPE, ClientType.class);
        TokenConfig config = jwtProperties.getTokens().get(clientType.getName());
        if (config == null) {
            response.setActive(false);
            return response;
        }

        Claims claims = JwtUtil.getClaimsBody(config.getSecretKey(), token);
        if (claims == null) {
            response.setActive(false);
            return response;
        }

        // 检查过期
        int val = JwtUtil.verifyToken(claims, config.getRefreshTime());
        if (val == 1 || val == 2) {
            response.setActive(false);
            return response;
        }

        // 有效token
        response.setActive(true);
        response.setSub(claims.getSubject());
        response.setClientId(claims.get("client_id", String.class));
        response.setScope(claims.get("scope", String.class));
        response.setTokenType(BEARER);
        response.setExp(claims.getExpiration().getTime() / 1000);
        response.setIat(claims.getIssuedAt().getTime() / 1000);
        response.setUserId(claims.get("userId", Long.class));
        response.setUsername(claims.get("username", String.class));
        response.setClientType(claims.get("clientType", ClientType.class));
        response.setRoles(claims.get(ROLES, List.class));
        response.setExp(claims.getExpiration().getTime() / 1000);
        response.setIat(claims.getIssuedAt().getTime() / 1000);
        response.setSub(claims.getSubject());
        response.setJti(claims.get("jti", String.class));
        response.setScope(String.join(" ", claims.get(ROLES, List.class)));

        return response;
    }

    /**
     * 用户信息
     *
     * @param operatorId
     * @return
     */
    @Override
    public UserInfoRES getUserInfo(Long operatorId) {
        AuthUserEO user = userMapper.selectById(operatorId);
        if (user == null) {
            throw USER_NOT_EXIST.toException();
        }
        List<String> roles = getRoles(user.getId());

        UserInfoRES response = new UserInfoRES();
        response.setSub(user.getId().toString());
        response.setPreferredUsername(user.getUsername());
        response.setName(user.getUsername());
        response.setPhone(user.getPhone());
        response.setEmail(user.getEmail());
        response.setUserId(user.getId());
        response.setRoles(roles);
        response.setUpdatedAt(user.getUpdateTime().toInstant(ZoneOffset.UTC).getEpochSecond());

        return response;
    }

    /**
     * OAuth2服务端元数据
     *
     * @return
     */
    @Override
    public AuthorizationServerMetadata getMetadata() {
        String issuer = StrUtil.isEmpty(busSystemProperties.getDomain()) ?
                HTTP + busSystemProperties.getIp() + COLON + busSystemProperties.getPort() :
                HTTPS + busSystemProperties.getDomain();
        return AuthorizationServerMetadata.builder()
            .issuer(issuer)
            .authorizationEndpoint(issuer + AUTHORIZATION_ENDPOINT)
            .tokenEndpoint(issuer + TOKEN_ENDPOINT)
            .userinfoEndpoint(issuer + USER_INFO_ENDPOINT)
            .tokenIntrospectionEndpoint(issuer + INTROSPECTION_ENDPOINT)
            .tokenRevocationEndpoint(issuer + REVOKE_TOKEN_ENDPOINT)
            .jwksUri(issuer + JWKS_ENDPOINT)
            .grantTypesSupported(GRANT_TYPES_SUPPORTED)
            .responseTypesSupported(RESPONSE_TYPES_SUPPORTED)
            .tokenEndpointAuthMethodsSupported(TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED)
            .scopesSupported(SCOPES_SUPPORTED)
            .codeChallengeMethodsSupported(CODE_CHALLENGE_METHODS_SUPPORTED)
            .build();
    }

    /**
     * 密码授权
     *
     * @param req
     * @return
     */
    @Override
    public TokenRES passwordGrant(OAuth2TokenREQ req) {
        String clientId = req.getClientId();
        String clientSecret = req.getClientSecret();
        String scope = req.getScope();
        Map<String, String> params = req.getExtraParams();
        String username = params.get(USERNAME);
        String password = params.get(PASSWORD);

        // 1. 校验客户端
        ClientConfig client = validateClient(clientId, clientSecret);
        if (!client.getGrantTypes().contains(PASSWORD_GRANT_TYPE)) {
            throw UNSUPPORTED_GRANT_TYPE.toException();
        }

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw PARAM_ERROR.toException();
        }

        // 2. 校验用户（支持用户名/手机号/邮箱）
        LambdaQueryWrapper<AuthUserEO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.eq(AuthUserEO::getUsername, username)
                    .or()
                    .eq(AuthUserEO::getPhone, username)
                    .or()
                    .eq(AuthUserEO::getEmail, username);
        }
        AuthUserEO user = userMapper.selectOne(wrapper);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw INVALID_GRANT.toException();
        }

        if (user.getStatus() == BaseStatus.DISABLE) {
            throw INVALID_GRANT.toException();
        }

        // 3. 校验 scope
        String finalScope = validateScope(scope, client.getScopes());

        // 4. 生成令牌
        LoginTokenRES res = LoginTokenRES.builder()
                .id(user.getId())
                .username(user.getUsername())
                .build();
        TokenRES tokenRES = generateUserToken(user.getId(), clientId, finalScope);
        BeanUtils.copyProperties(tokenRES, res);
        return res;
    }

    /**
     * 授权码授权
     *
     * @param req
     * @return
     */
    @Override
    public TokenRES authorizationCodeGrant(OAuth2TokenREQ req) {
        String clientId = req.getClientId();
        String clientSecret = req.getClientSecret();
        Map<String, String> params = req.getExtraParams();
        String code = params.get(CODE);
        String redirectUri = params.get(REDIRECT_URI);
        String codeVerifier = params.get(CODE_VERIFIER);

        // 1. 校验客户端
        ClientConfig client = validateClient(clientId, clientSecret);
        if (!client.getGrantTypes().contains(AUTHORIZATION_CODE_GRANT_TYPE)) {
            throw UNSUPPORTED_GRANT_TYPE.toException();
        }

        // 2. 获取并删除授权码（一次性）
        String key = OAUTH2_CODE_PREFIX + code;
        String codeDataJson = stringRedisTemplate.opsForValue().get(key);
        if (codeDataJson == null) {
            throw INVALID_GRANT.toException();
        }
        stringRedisTemplate.delete(key);

        AuthorizationCodeData codeData = JSON.parseObject(codeDataJson, AuthorizationCodeData.class);

        // 3. 校验授权码信息
        if (!codeData.getClientId().equals(clientId) ||
            !codeData.getRedirectUri().equals(redirectUri)) {
            throw INVALID_GRANT.toException();
        }

        // 4. PKCE 验证（如有）
        if (codeData.getCodeChallenge() != null) {
            verifyPKCE(codeVerifier, codeData.getCodeChallenge(), codeData.getCodeChallengeMethod());
        }

        // 5. 生成令牌
        return generateUserToken(codeData.getUserId(), clientId, codeData.getScope());
    }

    /**
     * 客户端授权
     *
     * @param req
     * @return
     */
    @Override
    public TokenRES clientCredentialsGrant(OAuth2TokenREQ req) {
        String clientId = req.getClientId();
        String clientSecret = req.getClientSecret();
        String scope = req.getScope();

        // 1. 校验客户端
        ClientConfig client = validateClient(clientId, clientSecret);
        if (!client.getGrantTypes().contains(CLIENT_CREDENTIALS_GRANT_TYPE)) {
            throw UNSUPPORTED_GRANT_TYPE.toException();
        }

        // 2. 校验 scope
        String finalScope = validateScope(scope, client.getScopes());

        return generateUserToken(getOperatorId(), clientId, finalScope);
    }

    /**
     * 刷新令牌授权
     *
     * @param req
     * @return
     */
    @Override
    public TokenRES refreshTokenGrant(OAuth2TokenREQ req) {
        Map<String, String> params = req.getExtraParams();
        String clientId = req.getClientId();
        String clientSecret = req.getClientSecret();
        String scope = req.getScope();
        String refreshToken = params.get(REFRESH_TOKEN_GRANT_TYPE);

        // 1. 校验客户端
        ClientConfig client = validateClient(clientId, clientSecret);
        if (!client.getGrantTypes().contains(REFRESH_TOKEN_GRANT_TYPE)) {
            throw UNSUPPORTED_GRANT_TYPE.toException();
        }

        // 2. 解析 refresh_token
        ClientType clientType = getExtra(CLIENT_TYPE, ClientType.class);
        TokenConfig tokenConfig = jwtProperties.getTokens().get(clientType.getName());
        Claims claims = JwtUtil.getClaimsBody(tokenConfig.getSecretKey(), refreshToken);
        if (claims == null) {
            throw INVALID_GRANT.toException();
        }
        int val = JwtUtil.verifyToken(claims, tokenConfig.getRefreshTime());
        if (val > 0) {
            throw JWT_EXPIRED.toException();
        }

        // 3. 检查黑名单
        if (isTokenBlacklisted(refreshToken)) {
            throw INVALID_GRANT.toException();
        }

        // 4. 获取用户
        Long userId = Long.valueOf(claims.getSubject());

        // 5. 可选：校验 scope 不扩大
        String finalScope = scope != null ? validateScope(scope, client.getScopes())
                                          : claims.get(SCOPE, String.class);

        // 6. 生成新令牌对
        return generateUserToken(userId, clientId, finalScope);
    }

    /**
     * 微信小程序授权
     * @param request
     * @return
     */
    @Override
    public TokenRES wechatMiniGrant(OAuth2TokenREQ request){
        Map<String, String> params = request.getExtraParams();
        String clientId = request.getClientId();
        String clientSecret = request.getClientSecret();
        String scope = request.getScope();
        // 1. 校验客户端
        ClientConfig client = validateClient(clientId, clientSecret);
        if (!client.getGrantTypes().contains(CLIENT_CREDENTIALS_GRANT_TYPE)) {
            throw UNSUPPORTED_GRANT_TYPE.toException();
        }

        // 2. 校验 scope
        String finalScope = validateScope(scope, client.getScopes());

        WechatLoginREQ req = WechatLoginREQ.builder()
            .code(params.get(WX_CODE))
            .userInfo(UserInfoREQ.builder()
                    .nickname(params.get(WX_NICK_NAME))
                    .avatarUrl(params.get(WX_AVATAR_URL))
                    .build())
            .encryptedData(params.get(WX_ENCRYPTED_DATA))
            .iv(params.get(WX_IV))
            .build();

        WxLoginTokenRES res = authUserService.wechatLogin(req);
        TokenRES tokenRES = generateUserToken(getOperatorId(), request.getClientId(), finalScope);
        BeanUtils.copyProperties(tokenRES, res);
        return res;
    }

    private void handleCodeFlow(Long userId, String clientId, String redirectUri,
                                String scope, String state, String codeChallenge,
                                String codeChallengeMethod, HttpServletResponse response) throws IOException {

        ClientConfig client = oAuth2ClientProperties.getClient(clientId)
            .orElseThrow(INVALID_CLIENT::toException);

        if (!client.getGrantTypes().contains(AUTHORIZATION_CODE_GRANT_TYPE)) {
            throw UNSUPPORTED_GRANT_TYPE.toException();
        }

        if (!isValidRedirectUri(redirectUri, client.getRedirectUris())) {
            throw INVALID_REDIRECT_URI.toException();
        }

        String finalScope = validateScope(scope, client.getScopes());

        // 生成授权码
        String authCode = generateAuthorizationCode();

        AuthorizationCodeData codeData = AuthorizationCodeData.builder()
            .code(authCode)
            .userId(userId)
            .clientId(clientId)
            .redirectUri(redirectUri)
            .scope(finalScope)
            .codeChallenge(codeChallenge)
            .codeChallengeMethod(codeChallengeMethod)
            .createTime(LocalDateTime.now())
            .build();

        stringRedisTemplate.opsForValue().set(
            OAUTH2_CODE_PREFIX + authCode,
            JSON.toJSONString(codeData),
            AUTH_CODE_TTL_MINUTES,
            TimeUnit.MINUTES
        );

        // 构建重定向 URL
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectUri)
            .queryParam("code", authCode);

        if (state != null) {
            builder.queryParam("state", state);
        }

        response.sendRedirect(builder.build().toUriString());
    }

    private ClientConfig validateClient(String clientId, String clientSecret) {
        ClientConfig client = oAuth2ClientProperties.getClient(clientId)
            .orElseThrow(INVALID_CLIENT::toException);

        // 校验密钥
        String encodedSecret = client.getClientSecret();
        if (encodedSecret.startsWith(SECRET_PREFIX)) {
            if (!passwordEncoder.matches(clientSecret, encodedSecret.substring(SECRET_PREFIX.length()))) {
                throw INVALID_CLIENT.toException();
            }
        } else if (!encodedSecret.equals(clientSecret)) {
            throw INVALID_CLIENT.toException();
        }

        return client;
    }

    private TokenRES generateUserToken(Long userId, String clientId, String scope) {
        ClientType endpoint = getExtra(CLIENT_TYPE, ClientType.class);
        TokenConfig tokenConfig = jwtProperties.getTokens().get(endpoint.getName());

        // Access Token
        Map<String, Object> accessClaims = new HashMap<>();
        accessClaims.put(SUB, userId.toString());
        accessClaims.put(CLIENT_ID, clientId);
        accessClaims.put(SCOPE, scope);

        // 业务字段
        accessClaims.put(OPERATOR_ID, userId);
        accessClaims.put(OPERATOR_NAME, getOperatorName());
        accessClaims.put(CLIENT_TYPE, endpoint.getName());
        accessClaims.put(ROLES, getRoles(userId));

        String accessToken = TOKEN_HEADER + JwtUtil.getToken(tokenConfig.getSecretKey(), tokenConfig.getTtl(), accessClaims);

        // Refresh Token（仅包含用户标识）
        Map<String, Object> refreshClaims = new HashMap<>();
        refreshClaims.put(SUB, userId.toString());

        String refreshToken = TOKEN_HEADER + JwtUtil.getToken(tokenConfig.getSecretKey(), tokenConfig.getTtl(), refreshClaims);

        // 保存token至redis
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        String redisKey = USER_TOKEN_PREFIX + userId;
        operations.set(redisKey, accessToken, tokenConfig.getRefreshTime(), TimeUnit.SECONDS);
        operations.set(redisKey, refreshToken, tokenConfig.getRefreshTime(), TimeUnit.SECONDS);

        return TokenRES.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType(BEARER)
            .expiresIn(tokenConfig.getTtl())
            .scope(scope)
            .build();
    }

    private String generateAuthorizationCode() {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(UUID.randomUUID().toString().getBytes());
    }

    private boolean isValidRedirectUri(String redirectUri, List<String> registeredUris) {
        return registeredUris != null && registeredUris.contains(redirectUri);
    }

    private String validateScope(String requested, List<String> allowed) {
        if (requested == null || requested.isEmpty()) {
            return String.join(" ", allowed);
        }
        Set<String> reqSet = Set.of(requested.split(" "));
        Set<String> allowSet = new HashSet<>(allowed);
        if (!allowSet.containsAll(reqSet)) {
            throw INVALID_SCOPE.toException();
        }
        return requested;
    }

    private void verifyPKCE(String verifier, String challenge, String method) {
        if (verifier == null) {
            throw INVALID_GRANT.toException();
        }

        if (method == null) {
            throw UNSUPPORTED_CODE_CHALLENGE_METHODS.toException();
        }

        String computed = "";
        if (CODE_CHALLENGE_METHOD_S256.equalsIgnoreCase(method)) {
            computed = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(DigestUtils.sha256(verifier));
        } else {
            throw UNSUPPORTED_CODE_CHALLENGE_METHODS.toException();
        }

        if (!computed.equals(challenge)) {
            throw INVALID_GRANT.toException();
        }
    }

    private boolean isTokenBlacklisted(String token) {
        return stringRedisTemplate.hasKey(OAUTH2_BLACKLIST_PREFIX + token);
    }

    private List<String> getRoles(Long userId) {
        List<RoleEO> rolesByUserId = authRoleService.getRolesByUserId(userId);
        return rolesByUserId.stream().map(RoleEO::getRoleCode).toList();
    }
}
