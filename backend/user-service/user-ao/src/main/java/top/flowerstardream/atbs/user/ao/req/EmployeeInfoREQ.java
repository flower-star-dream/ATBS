package top.flowerstardream.atbs.user.ao.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author: 花海
 * @Date: 2025/11/06/21:20
 * @Description: 员工信息请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "员工信息请求")
public class EmployeeInfoREQ implements Serializable {

    @Schema(description = "员工ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "别名")
    private String nickname;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "权限等级")
    private String permissionLevel;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "所属站点")
    private String affiliatedSite;

}
