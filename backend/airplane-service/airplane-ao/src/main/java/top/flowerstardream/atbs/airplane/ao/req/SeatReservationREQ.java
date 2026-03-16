package top.flowerstardream.atbs.airplane.ao.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author: QAQ
 * @Date: 2025/11/09/23:00
 * @Description: 座位预订请求
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "座位预订请求")
public class SeatReservationREQ implements Serializable {

    @Schema(description = "座位预订号")
    private Long id;

    @Schema(description = "班次id")
    private Long scheduleId;

    @Schema(description = "座位号")
    private Integer seatNum;

    @Schema(description = "预订状态")
    private Integer bookingStatus;

}
