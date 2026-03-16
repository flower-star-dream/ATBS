package top.flowerstardream.atbs.order.ao.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.atbs.order.common.enums.TicketStatus;
import top.flowerstardream.base.ao.req.BaseStatusChangeREQ;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author: 花海
 * @Date: 2025/11/13/20:11
 * @Description: 机票状态变更请求
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "机票状态变更请求")
public class TicketStatusChangeREQ extends BaseStatusChangeREQ<TicketStatus> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "班次ID")
    private Long scheduleId;

    @Schema(description = "出发站ID")
    private Long startStationId;

    @Schema(description = "到达站ID")
    private Long endStationId;
}
