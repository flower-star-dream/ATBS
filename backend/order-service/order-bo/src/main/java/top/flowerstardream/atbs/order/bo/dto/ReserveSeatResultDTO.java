package top.flowerstardream.atbs.order.bo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author: 花海
 * @Date: 2025/11/13/16:27
 * @Description: 预定座位结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "预定座位结果")
public class ReserveSeatResultDTO {

    @Schema(description = "预定座位id列表")
    private List<Long> seatReservationIds;

    @Schema(description = "出发时间")
    private LocalDateTime startTime;

    @Schema(description = "到达时间")
    private LocalDateTime endTime;
}
