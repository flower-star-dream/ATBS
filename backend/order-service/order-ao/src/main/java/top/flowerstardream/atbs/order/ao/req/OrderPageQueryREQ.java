package top.flowerstardream.atbs.order.ao.req;

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
 * @Date: 2025/11/11
 * @Description: 订单分页查询请求
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "订单分页查询请求")
public class OrderPageQueryREQ extends BasePageQueryREQ implements Serializable {

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String username;

    /**
     * 订单状态
     */
    @Schema(description = "订单号")
    private Long id;
}