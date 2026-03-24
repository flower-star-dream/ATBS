package top.flowerstardream.atbs.auth.ao.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author: 花海
 * @Date: 2025/10/28/17:14
 * @Description: 登录返回参数
 */
@Schema(description = "登录返回参数")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LoginTokenRES extends TokenRES {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "员工id")
    private Long id;

    @Schema(description = "用户名")
    private String username;

}
