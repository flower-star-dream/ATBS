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
 * @Date: 2025/11/14/03:30
 * @Description: 座位预约信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "座位预约信息")
public class SeatReservationDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "座位ID")
    private Long id;

    @Schema(description = "班次号")
    private Long scheduleId;

    @Schema(description = "座位号")
    private Integer seatNum;
}
