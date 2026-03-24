package top.flowerstardream.atbs.auth.ao.req;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * OAuth2Token请求参数
 * @author 花海
 * @date 2026/03/18
 * @description OAuth2Token请求参数
 */
@Data
@Builder
public class OAuth2TokenREQ {
    private String grantType;
    private String clientId;
    private String clientSecret;
    private String scope;
    private Map<String, String> extraParams;
}