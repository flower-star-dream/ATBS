package top.flowerstardream.atbs.order.ao.req;

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
 * @Description: 机票请求对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketREQ implements Serializable {

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
     * 票价
     */
    @Schema(description = "票价")
    private BigDecimal money;

    /**
     * 出发时间
     */
    @Schema(description = "出发时间")
    private LocalDateTime startTime;

    /**
     * 到达时间
     */
    @Schema(description = "到达时间")
    private LocalDateTime endTime;

    /**
     * 出发站
     */
    @Schema(description = "出发站")
    private String startStation;

    /**
     * 到达站
     */
    @Schema(description = "到达站")
    private String endStation;

    /**
     * 乘车日期
     */
    @Schema(description = "乘车日期")
    private LocalDateTime rideDate;
}