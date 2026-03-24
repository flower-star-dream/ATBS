package top.flowerstardream.atbs.auth.ao.res;// ==================== Token 内省响应 (RFC 7662) ====================

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import top.flowerstardream.atbs.tools.constants.ClientType;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Token 内省响应 (RFC 7662)
 * @Author: 花海
 * @Date: 2026/03/17/15:44
 * @Description: Token 内省响应
 */
@Data
public class IntrospectRES implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    // 必需：token是否有效
    private Boolean active;
    
    @JsonProperty("client_id")
    private String clientId;
    
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("token_type")
    private String tokenType;
    
    // 过期时间戳
    private Long exp;
    // 签发时间戳
    private Long iat;
    // 生效时间戳
    private Long nbf;
    // 用户标识
    private String sub;
    // 受众
    private String aud;
    // 签发者
    private String iss;
    // JWT ID
    private String jti;
    
    @JsonProperty("scope")
    private String scope;
    
    // 扩展字段
    @JsonProperty("user_id")
    private Long userId;
    
    @JsonProperty("client_type")
    private ClientType clientType;
    
    @JsonProperty("roles")
    private List<String> roles;
}