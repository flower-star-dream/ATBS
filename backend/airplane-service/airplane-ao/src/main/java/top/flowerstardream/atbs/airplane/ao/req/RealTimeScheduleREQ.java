package top.flowerstardream.atbs.airplane.ao.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Author: 花海
 * @Date: 2025/11/29/17:48
 * @Description: 实时车次查询请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "实时车次查询请求参数")
public class RealTimeScheduleREQ implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "班次ID")
    private Long scheduleId;

    @Schema(description = "用户出发时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nowTime;

    @Schema(description = "用户出发站ID")
    private Long startStationId;

    @Schema(description = "用户到达站ID")
    private Long endStationId;

}
