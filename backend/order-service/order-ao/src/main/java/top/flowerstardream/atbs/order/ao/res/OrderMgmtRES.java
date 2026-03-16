package top.flowerstardream.atbs.order.ao.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.atbs.order.common.enums.OrderStatus;
import top.flowerstardream.base.ao.res.BaseMgmtRES;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @Author: 花海
 * @Date: 2025/11/21/16:31
 * @Description: 订单后管响应
 */

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "订单后管响应")
public class OrderMgmtRES extends BaseMgmtRES {
    
    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "订单状态")
    private OrderStatus status;

    @Schema(description = "订单备注")
    private String remarks;

    @Schema(description = "订单总价")
    private BigDecimal totalPrice;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "已付金额")
    private BigDecimal amountPaid;
}
