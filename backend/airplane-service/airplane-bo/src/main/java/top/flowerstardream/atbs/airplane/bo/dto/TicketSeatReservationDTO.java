package top.flowerstardream.atbs.airplane.bo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "有关座位预订信息的车票数据传输对象")
public class TicketSeatReservationDTO {

    /**
     * 票号
     */
    @Schema(description = "票号")
    private Long Id;

    /**
     * 座位预订ID
     */
    @Schema(description = "座位预订ID")
    private Long seatReservationId;
}
