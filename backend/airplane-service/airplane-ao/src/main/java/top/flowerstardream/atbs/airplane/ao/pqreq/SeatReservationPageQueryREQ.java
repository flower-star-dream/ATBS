package top.flowerstardream.atbs.airplane.ao.pqreq;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.ao.req.BasePageQueryREQ;
import top.flowerstardream.base.state.BaseStatus;

import java.io.Serial;
import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "座位预约请求")
public class SeatReservationPageQueryREQ extends BasePageQueryREQ {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description= "座位预订号")
    private Long id;

    @Schema(description= "班次id")
    private Long scheduleId;

    @Schema(description= "座位号")
    private Integer seatNum;

    @Schema(description= "预订状态")
    private BaseStatus status;
}
