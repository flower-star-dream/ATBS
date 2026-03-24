package top.flowerstardream.atbs.auth.ao.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.flowerstardream.base.state.BaseStatus;

/**
 * @Author: 花海
 * @Date: 2026/03/19/02:18
 * @Description: 用户新增/修改参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户新增/修改请求")
public class AuthUserREQ {
    @Schema(description = "统一用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "状态")
    private BaseStatus status;
}
