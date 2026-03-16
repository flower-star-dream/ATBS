package top.flowerstardream.atbs.order.bo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.flowerstardream.atbs.order.common.enums.OrderStatus;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author: 花海
 * @Date: 2025/11/21/22:25
 * @Description: 取消机票请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "取消机票请求")
public class CancelTicketDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID")
    Long orderId;

    @Schema(description = "状态")
    OrderStatus status;
}
