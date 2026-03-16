package top.flowerstardream.atbs.order.ao.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.atbs.order.common.enums.OrderStatus;
import top.flowerstardream.base.ao.req.BaseStatusChangeREQ;

import java.io.Serializable;

/**
 * @Author: 花海
 * @Date: 2025/11/11
 * @Description: 订单状态修改请求
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "订单状态修改请求")
public class OrderStatusREQ extends BaseStatusChangeREQ<OrderStatus> implements Serializable {

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remarks;
}