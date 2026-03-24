package top.flowerstardream.atbs.auth.ao.res;// ==================== 用户信息响应 (OIDC) ====================

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户信息响应 (OIDC)
 * @Author: 花海
 * @Date: 2026/03/17/15:44
 * @Description: 用户信息响应
 */
@Data
public class UserInfoRES implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // 用户唯一标识
    private String sub;
    
    @JsonProperty("preferred_username")
    private String preferredUsername;
    
    private String name;
    private String phone;
    private String email;
    private String picture;
    
    @JsonProperty("updated_at")
    private Long updatedAt;
    
    // 扩展
    @JsonProperty("user_id")
    private Long userId;
    
    @JsonProperty("client_type")
    private String clientType;
    
    @JsonProperty("roles")
    private List<String> roles;
}
