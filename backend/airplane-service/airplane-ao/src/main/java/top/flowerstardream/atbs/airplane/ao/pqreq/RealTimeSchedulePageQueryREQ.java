package top.flowerstardream.atbs.airplane.ao.pqreq;


import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
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
@Schema(description = "实时班次查询请求")
public class RealTimeSchedulePageQueryREQ extends BasePageQueryREQ {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户出发时间")
    @JsonFormat(pattern = "yyyy-MM-ddTHH:mm:ss")
    private LocalDateTime nowTime;

    @Schema(description = "用户出发站ID")
    private Long startStationId;

    @Schema(description = "用户到达站ID")
    private Long endStationId;




}
