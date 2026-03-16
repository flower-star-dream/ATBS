package top.flowerstardream.atbs.user.ao.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.ao.req.BasePageQueryREQ;

import java.io.Serializable;

/**
 * @Author: 花海
 * @Date: 2025/11/08/14:51
 * @Description: 员工分页查询请求
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "员工分页查询请求")
public class EmployeePageQueryREQ extends BasePageQueryREQ implements Serializable {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "手机号")
    private String phone;

}
