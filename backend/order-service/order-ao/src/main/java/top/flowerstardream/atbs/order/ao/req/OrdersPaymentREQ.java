package top.flowerstardream.atbs.order.ao.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.flowerstardream.atbs.order.common.enums.OrderStatus;

/**
 * @Author: 花海
 * @Date: 2025/11/21/15:35
 * @Description: 订单支付请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单支付请求参数")
public class OrdersPaymentREQ {

    //订单号
    @Schema(description = "订单号")
    private Long orderId;

    /**
     * 订单状态
     */
    @Schema(description = "订单状态")
    private OrderStatus status;

}
