package top.flowerstardream.atbs.airplane.bo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author: 花海
 * @Date: 2025/11/12/22:47
 * @Description: 计算车票价格参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "计算车票价格参数")
public class CalcTicketPriceDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "班次ID")
    private Long scheduleId;

    @Schema(description = "出发站ID")
    private Long startStationId;

    @Schema(description = "到达站ID")
    private Long endStationId;
}
