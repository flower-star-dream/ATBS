package top.flowerstardream.atbs.user.ao.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import top.flowerstardream.base.state.BaseStatus;

import java.io.Serializable;

import static top.flowerstardream.base.state.BaseStatus.DISABLE;

/**
 * @Author: 花海
 * @Date: 2025/11/08/16:46
 * @Description: 员工新增/修改参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "员工新增/修改请求")
public class EmployeeREQ implements Serializable {
    @Schema(description = "员工ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "昵称")
    private String nickname ;

    @Schema(description = "密码")
    private String password ;

    @Schema(description = "所属站点")
    private String affiliatedSite ;

    @Schema(description = "状态")
    private BaseStatus status;
}
