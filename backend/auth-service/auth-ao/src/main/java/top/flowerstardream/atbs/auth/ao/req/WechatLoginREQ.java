package top.flowerstardream.atbs.auth.ao.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 微信登录请求
 *
 * @author 花海
 * @date 2025-11-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "微信登录请求")
public class WechatLoginREQ implements Serializable {

    @Schema(description = "微信登录code")
    @NotBlank(message = "微信code不能为空")
    private String code;
    
    @Schema(description = "用户信息")
    private UserInfoREQ userInfo;
}