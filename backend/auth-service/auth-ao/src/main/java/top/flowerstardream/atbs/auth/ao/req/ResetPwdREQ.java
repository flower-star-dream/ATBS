package top.flowerstardream.atbs.auth.ao.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author: 花海
 * @Date: 2025/11/09/01:04
 * @Description: 重置密码请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "重置密码请求")
public class ResetPwdREQ implements Serializable {
    @Schema(description = "员工id")
    private Long id;

    @Schema(description = "旧密码")
    private String oldPwd;

    @Schema(description = "新密码")
    private String newPwd;

    @Schema(description = "确认密码")
    private String confirmPwd;
}
