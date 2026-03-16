package top.flowerstardream.atbs.airplane.ao.res;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.ao.res.BaseMgmtRES;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Author: QAQ
 * @Date: 2025/11/09/23:00
 * @Description: 班次返回参数
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "班次返回参数")
public class ScheduleRES extends BaseMgmtRES {

    @Schema(description = "飞机名")
    private String airplaneName;

    @Schema(description = "路线名")
    private String routeName;

    @Schema(description = "飞机长")
    private String captain;

    @Schema(description = "余票")
    private Integer availableTickets;

    @Schema(description = "出发时间")
    private LocalDateTime startTime;

    @Schema(description = "到达时间")
    private LocalDateTime endTime;
}
