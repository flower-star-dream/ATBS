package top.flowerstardream.atbs.user.ao.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;

/**
 * @Author: 花海
 * @Date: 2025/11/08/16:46
 * @Description: 管理员新增/修改参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理员新增/修改请求")
public class AdminREQ implements Serializable {

    @Schema(description = "管理员ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "权限等级")
    private String permissionLevel;

    @Schema(description = "昵称")
    private String nickname ;

    @Schema(description = "密码")
    private String password ;

    @Schema(description = "所属站点")
    private String affiliatedSite ;
}
