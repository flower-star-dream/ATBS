package top.flowerstardream.atbs.order.bo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author: 花海
 * @Date: 2024/11/11
 * @Description: 机票数据传输对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "机票数据传输对象")
public class TicketDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 班次ID
     */
    @Schema(description = "班次ID")
    private Long scheduleId;

    /**
     * 所属订单号
     */
    @Schema(description = "所属订单号")
    private Long orderId;

    /**
     * 乘车人ID列表
     */
    @Schema(description = "乘车人ID列表")
    private List<Long> passengerIds;

    /**
     * 价格
     */
    @Schema(description = "价格")
    private BigDecimal money;

    /**
     * 出发站
     */
    @Schema(description = "出发站ID")
    private Long startStationId;

    /**
     * 到达站
     */
    @Schema(description = "到达站ID")
    private Long endStationId;
}