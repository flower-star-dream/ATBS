package top.flowerstardream.atbs.auth.ao.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新令牌请求DTO
 */
@Data
public class RefreshTokenRequest {

    /**
     * 刷新令牌
     */
    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
}
