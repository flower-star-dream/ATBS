package top.flowerstardream.atbs.auth.biz.service;

import jakarta.servlet.http.HttpServletResponse;
import top.flowerstardream.atbs.auth.ao.req.OAuth2AuthorizeREQ;
import top.flowerstardream.atbs.auth.ao.req.OAuth2TokenREQ;
import top.flowerstardream.atbs.auth.ao.res.AuthorizationServerMetadata;
import top.flowerstardream.atbs.auth.ao.res.IntrospectRES;
import top.flowerstardream.atbs.auth.ao.res.TokenRES;
import top.flowerstardream.atbs.auth.ao.res.UserInfoRES;
import top.flowerstardream.atbs.auth.bo.eo.AuthUserEO;
import top.flowerstardream.base.service.IBaseService;

import java.io.IOException;
import java.util.Map;

/**
 * @Author: 花海
 * @Date: 2026/03/17/15:44
 * @Description: OAuth2服务接口
 */
public interface IOAuth2Service {
    /**
     * 授权端点
     * @param req
     * @param response
     */
    void authorize(OAuth2AuthorizeREQ req, HttpServletResponse response) throws IOException;

    /**
     * 令牌撤销
     * @param token
     * @param tokenTypeHint
     */
    void revokeToken(String token, String tokenTypeHint);

    /**
     * 令牌内省
     * @param token
     * @param tokenTypeHint
     * @return
     */
    IntrospectRES introspectToken(String token, String tokenTypeHint);

    /**
     * 用户信息
     * @param operatorId
     * @return
     */
    UserInfoRES getUserInfo(Long operatorId);

    /**
     * OAuth2服务端元数据
     * @return
     */
    AuthorizationServerMetadata getMetadata();

    /**
     * 密码授权
     * @param req
     * @return
     */
    TokenRES passwordGrant(OAuth2TokenREQ req);

    /**
     * 授权码授权
     * @param req
     * @return
     */
    TokenRES authorizationCodeGrant(OAuth2TokenREQ req);

    /**
     * 客户端授权
     * @param req
     * @return
     */
    TokenRES clientCredentialsGrant(OAuth2TokenREQ req);

    /**
     * 刷新令牌授权
     * @param req
     * @return
     */
    TokenRES refreshTokenGrant(OAuth2TokenREQ req);

    /**
     * 微信小程序授权
     * @param request
     * @return
     */
    TokenRES wechatMiniGrant(OAuth2TokenREQ request);
}
