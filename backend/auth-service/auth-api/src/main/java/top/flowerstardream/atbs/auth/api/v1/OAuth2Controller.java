package top.flowerstardream.atbs.auth.api.v1;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.auth.ao.req.OAuth2AuthorizeREQ;
import top.flowerstardream.atbs.auth.ao.req.OAuth2TokenREQ;
import top.flowerstardream.atbs.auth.ao.res.AuthorizationServerMetadata;
import top.flowerstardream.atbs.auth.ao.res.IntrospectRES;
import top.flowerstardream.atbs.auth.ao.res.TokenRES;
import top.flowerstardream.atbs.auth.ao.res.UserInfoRES;
import top.flowerstardream.atbs.auth.biz.service.IOAuth2Service;
import top.flowerstardream.base.utils.JwtUtil;

import java.io.IOException;
import java.util.Map;

import static top.flowerstardream.atbs.auth.common.AuthExceptionEnum.*;
import static top.flowerstardream.atbs.auth.common.OAuth2Constant.*;
import static top.flowerstardream.base.utils.GetInfoUtil.getOperatorId;

/**
 * 认证控制器
 * @Author: 花海
 * @Date: 2026/03/17/15:44
 * @Description: 全局认证控制器
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "OAuth2认证中心")
public class OAuth2Controller {

    @Resource
    private IOAuth2Service oauth2Service;

    // ==================== OAuth2 标准端点 ====================

    /**
     * 授权端点 - RFC 6749 4.1.1
     * GET /oauth/authorize?response_type=code&client_id=xxx&redirect_uri=xxx&scope=xxx&state=xxx
     */
    @Operation(summary = "OAuth2授权端点")
    @GetMapping(AUTHORIZATION_ENDPOINT)
    public void authorize(
        @RequestParam(RESPONSE_TYPE) String responseType,  // code/token(deprecated)
        @RequestParam(CLIENT_ID) String clientId,
        @RequestParam(value = REDIRECT_URI, required = false) String redirectUri,
        @RequestParam(value = SCOPE, required = false) String scope,
        @RequestParam(value = STATE, required = false) String state,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException{
        OAuth2AuthorizeREQ req = OAuth2AuthorizeREQ.builder()
            .responseType(responseType)
            .clientId(clientId)
            .redirectUri(redirectUri)
            .scope(scope)
            .state(state)
            .codeChallenge(request.getParameter(CODE_CHALLENGE))
            .codeChallengeMethod(request.getParameter(CODE_CHALLENGE_METHOD))
            .build();
        // 重定向到登录页或第三方授权
        oauth2Service.authorize(req, response);
    }

    /**
     * 令牌端点 - RFC 6749 3.2
     * POST /oauth/token
     * Content-Type: application/x-www-form-urlencoded
     */
    @Operation(summary = "OAuth2令牌端点")
    @PostMapping(
        value = TOKEN_ENDPOINT,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public TokenRES token(@RequestParam(GRANT_TYPE) String grantType,      // authorization_code/password/client_credentials/refresh_token
                          @RequestParam Map<String, String> parameters       // 其他参数动态接收
                                ){
        OAuth2TokenREQ request = OAuth2TokenREQ.builder()
        .grantType(parameters.get(GRANT_TYPE))
        .clientId(parameters.get(CLIENT_ID))
        .clientSecret(parameters.get(CLIENT_SECRET))
        .scope(parameters.get(SCOPE))
        .extraParams(parameters)
        .build();

        // 根据 grant_type 分发
        return switch (grantType) {
            case AUTHORIZATION_CODE_GRANT_TYPE -> oauth2Service.authorizationCodeGrant(request);
            case PASSWORD_GRANT_TYPE -> oauth2Service.passwordGrant(request);
            case WECHAT_MINI_GRANT_TYPE -> oauth2Service.wechatMiniGrant(request);
            case CLIENT_CREDENTIALS_GRANT_TYPE -> oauth2Service.clientCredentialsGrant(request);
            case REFRESH_TOKEN_GRANT_TYPE -> oauth2Service.refreshTokenGrant(request);
            default -> throw UNSUPPORTED_GRANT_TYPE.toException();
        };
    }

    /**
     * 令牌撤销 - RFC 7009
     * POST /oauth/revoke
     */
    @Operation(summary = "OAuth2令牌撤销")
    @PostMapping(
        value = REVOKE_TOKEN_ENDPOINT,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public void revoke(@RequestParam("token") String token,
                        @RequestParam(value = "token_type_hint", required = false) String tokenTypeHint  // access_token/refresh_token
                    ){
        oauth2Service.revokeToken(token, tokenTypeHint);
    }

    /**
     * 令牌内省 - RFC 7662（仅内部服务）
     * POST /oauth/introspect
     */
    @Operation(summary = "OAuth2令牌内省")
    @PostMapping(
        value = INTROSPECTION_ENDPOINT,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    @PreAuthorize("hasRole('SYSTEM')")
    public IntrospectRES introspect(@RequestParam("token") String token,
                                    @RequestParam(value = "token_type_hint", required = false) String tokenTypeHint){
        // 内部服务调用，验证token并返回完整信息
        return oauth2Service.introspectToken(token, tokenTypeHint);
    }

    /**
     * 用户信息 - OIDC标准
     * GET /userinfo
     */
    @Operation(summary = "OIDC用户信息")
    @GetMapping(USER_INFO_ENDPOINT)
    @PreAuthorize("isAuthenticated()")
    public UserInfoRES userinfo(){
        return oauth2Service.getUserInfo(getOperatorId());
    }

    // ==================== 扩展端点（遵循项目规范路径） ====================

    /**
     * JWK Set - RFC 7517
     * GET /.well-known/jwks.json
     */
    @Operation(summary = "JWK公钥集")
    @GetMapping(JWKS_ENDPOINT)
    public Map<String, Object> jwks(){
        return JwtUtil.exportJwks();
    }

    /**
     * OAuth2服务端元数据 - RFC 8414
     * GET /.well-known/oauth-authorization-server
     */
    @Operation(summary = "OAuth2服务端配置")
    @GetMapping("/.well-known/oauth-authorization-server")
    public AuthorizationServerMetadata metadata(){
        return oauth2Service.getMetadata();
    }
}
