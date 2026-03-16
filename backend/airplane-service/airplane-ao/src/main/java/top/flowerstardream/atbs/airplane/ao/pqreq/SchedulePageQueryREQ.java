package top.flowerstardream.atbs.airplane.ao.pqreq;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.ao.req.BasePageQueryREQ;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "班次分页查询请求")
public class SchedulePageQueryREQ extends BasePageQueryREQ {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "班次号")
    private Long id;

    @Schema(description = "飞机ID")
    private Long airplaneId;

    @Schema(description = "路线ID")
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
