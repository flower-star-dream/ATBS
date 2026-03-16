package top.flowerstardream.atbs.order.ao.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author: 花海
 * @Date: 2025/11/11
 * @Description: 订单新增请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单新增请求")
public class OrderREQ implements Serializable {

    /**
     * 车次ID
     */
    @Schema(description = "班次ID")
    private Long scheduleId;

    /**
     * 出发站点ID
     */
    @Schema(description = "出发站点ID")
    private Long startStationId;

    /**
     * 到达站点ID
     */
    @Schema(description = "到达站点ID")
    private Long endStationId;

    /**
     * 乘车人ID列表
     */
    @Schema(description = "乘车人ID列表")
    private List<Long> passengerIds;

    /**
     * 订单备注
     */
    @Schema(description = "订单备注")
    private String remarks;
}