package top.flowerstardream.atbs.auth.ao.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author: 花海
 * @Date: 2025/10/28/17:14
 * @Description: 登录返回参数
 */
@Schema(description = "登录返回参数")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WxLoginRES implements Serializable {

    @Schema(description = "员工id")
    private Long id;

    @Schema(description = "微信openid")
    private String openid;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "jwt令牌")
    private String token;

}
