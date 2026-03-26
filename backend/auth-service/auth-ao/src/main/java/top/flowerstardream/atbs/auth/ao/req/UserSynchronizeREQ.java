package top.flowerstardream.atbs.auth.ao.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.flowerstardream.base.state.BaseStatus;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author: 花海
 * @Date: 2026/03/19/04:38
 * @Description: 同步用户信息请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "同步用户信息请求")
public class UserSynchronizeREQ implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户id")
    private Long id;

    @Schema(description = "微信openid")
    private String openid;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "权限等级/用户角色")
    private String permissionLevel;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "状态")
    private BaseStatus status;
}
