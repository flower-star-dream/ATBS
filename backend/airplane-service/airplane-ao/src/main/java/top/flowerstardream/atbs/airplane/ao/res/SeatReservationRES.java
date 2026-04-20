package top.flowerstardream.atbs.airplane.ao.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.ao.res.BaseMgmtRES;
import top.flowerstardream.base.state.BaseStatus;

import java.io.Serializable;

/**
 * @Author: QAQ
 * @Date: 2025/11/09/23:00
 * @Description: 座位预订返回参数
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "座位预订返回参数")
public class SeatReservationRES extends BaseMgmtRES {

    @Schema(description = "班次id")
    private Long scheduleId;

    @Schema(description = "飞机名")
    private String airplaneName;

    @Schema(description = "线路名")
    private String routeName;

    @Schema(description = "座位号")
    private Integer seatNum;

    @Schema(description = "预订状态")
    private BaseStatus bookingStatus;

}
