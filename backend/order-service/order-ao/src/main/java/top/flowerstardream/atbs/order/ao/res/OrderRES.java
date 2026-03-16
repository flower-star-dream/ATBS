package top.flowerstardream.atbs.order.ao.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @Author: 花海
 * @Date: 2025/11/11
 * @Description: 订单响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单响应")
public class OrderRES implements Serializable {

    /**
     * 订单ID
     */
    @Schema(description = "订单ID")
    private Long id;

    /**
     * 订单状态
     */
    @Schema(description = "订单状态")
    private Integer status;

    /**
     * 订单总价
     */
    @Schema(description = "订单总价")
    private BigDecimal totalPrice;

    /**
     * 已付金额
     */
    @Schema(description = "已付金额")
    private BigDecimal amountPaid;

    /**
     * 订单备注
     */
    @Schema(description = "订单备注")
    private String remarks;

    /**
     * 支付时间
     */
    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}