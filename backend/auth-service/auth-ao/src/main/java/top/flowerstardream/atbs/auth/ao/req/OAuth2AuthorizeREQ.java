package top.flowerstardream.atbs.auth.ao.req;

import lombok.Builder;
import lombok.Data;

/**
 * OAuth2授权请求参数
 * @author 花海
 * @date 2026/03/18/17:05
 * @description OAuth2授权请求参数
 */
@Data
@Builder
public class OAuth2AuthorizeREQ {
    private String responseType;
    private String clientId;
    private String redirectUri;
    private String scope;
    private String state;
    private String codeChallenge;
    private String codeChallengeMethod;
}