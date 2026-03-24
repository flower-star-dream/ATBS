package top.flowerstardream.atbs.auth.ao.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

import static top.flowerstardream.base.constant.CommonConstant.AUTHORIZATION;
import static top.flowerstardream.base.constant.CommonConstant.BEARER;

/**
 * Token 响应 (RFC 6749 5.1)
 * @Author: 花海
 * @Date: 2026/03/17/15:44
 * @Description: 令牌响应
 */

@Schema(description = "令牌响应参数")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRES implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("token_type")
    private String tokenType;
    
    @JsonProperty("expires_in")
    private Long expiresIn;
    
    @JsonProperty("refresh_token")
    private String refreshToken;
    
    @JsonProperty("scope")
    private String scope;
    
    // 扩展：指示需要刷新（非标准，但实用）
    @JsonProperty("refresh_recommended")
    private Boolean refreshRecommended;
}