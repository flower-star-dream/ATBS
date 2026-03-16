package top.flowerstardream.atbs.airplane.ao.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "实时班次查询响应")
public class RealTimeScheduleRES implements Serializable {

    @Schema(description = "班次ID")
    private Long scheduleId;

    @Schema(description = "出发时间")
    private LocalDateTime startTime;

    @Schema(description = "到达时间")
    private LocalDateTime endTime;

    @Schema(description = "出发站名")
    private String startStation;

    @Schema(description = "到达站名")
    private String endStation;

    @Schema(description = "票价")
    private BigDecimal price;

    @Schema(description = "余票")
    private Integer remainTicket;
}
