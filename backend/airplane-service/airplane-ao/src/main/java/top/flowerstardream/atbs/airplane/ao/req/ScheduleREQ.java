package top.flowerstardream.atbs.airplane.ao.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Author: QAQ
 * @Date: 2025/11/09/23:00
 * @Description: 班次请求
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "班次请求")
public class ScheduleREQ implements Serializable {

    @Schema(description = "班次号")
    private Long id;

    @Schema(description = "飞机号")
    private Long airplaneId;

    @Schema(description = "路线号")
    private Long routeId;

    @Schema(description = "飞机长")
    private String captain;

    @Schema(description = "出发时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "到达时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
