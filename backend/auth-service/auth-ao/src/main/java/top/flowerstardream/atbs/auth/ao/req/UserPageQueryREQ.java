package top.flowerstardream.atbs.auth.ao.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.annotation.Query;
import top.flowerstardream.base.ao.req.BasePageQueryREQ;

import java.io.Serializable;

/**
 * 用户分页查询请求
 *
 * @author 花海
 * @date 2025-11-15
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户分页查询请求")
public class UserPageQueryREQ extends BasePageQueryREQ implements Serializable {
    
    @Schema(description = "用户名")
    @Query.Condition("like")
    private String username;
    
    @Schema(description = "手机号")
    @Query.Condition("like")
    private String phone;
}