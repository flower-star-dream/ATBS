package top.flowerstardream.atbs.airplane.bo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单参数")
public class OrderDTO implements Serializable {

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "订单状态")
    private Integer status;

    @Schema(description = "订单备注")
    private String remarks;

    @Schema(description = "订单总价")
    private BigDecimal totalPrice;


}
