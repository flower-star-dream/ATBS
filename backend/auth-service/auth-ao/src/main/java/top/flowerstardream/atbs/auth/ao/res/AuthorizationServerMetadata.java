package top.flowerstardream.atbs.auth.ao.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 授权服务器元数据 (RFC 8414)
 * @Author: 花海
 * @Date: 2026/03/17/15:44
 * @Description: 授权服务器元数据
 */
@Data
@Builder
public class AuthorizationServerMetadata implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String issuer;
    
    @JsonProperty("authorization_endpoint")
    private String authorizationEndpoint;
    
    @JsonProperty("token_endpoint")
    private String tokenEndpoint;
    
    @JsonProperty("token_introspection_endpoint")
    private String tokenIntrospectionEndpoint;
    
    @JsonProperty("token_revocation_endpoint")
    private String tokenRevocationEndpoint;
    
    @JsonProperty("userinfo_endpoint")
    private String userinfoEndpoint;
    
    @JsonProperty("jwks_uri")
    private String jwksUri;
    
    @JsonProperty("registration_endpoint")
    private String registrationEndpoint;
    
    @JsonProperty("scopes_supported")
    private List<String> scopesSupported;
    
    @JsonProperty("response_types_supported")
    private List<String> responseTypesSupported;
    
    @JsonProperty("grant_types_supported")
    private List<String> grantTypesSupported;
    
    @JsonProperty("token_endpoint_auth_methods_supported")
    private List<String> tokenEndpointAuthMethodsSupported;

    @JsonProperty("code_challenge_methods_supported")
    private List<String> codeChallengeMethodsSupported;
}