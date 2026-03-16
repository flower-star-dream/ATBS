package top.flowerstardream.atbs.airplane.ao.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


/**
 * @Author: QAQ
 * @Date: 2025/11/06/23:00
 * @Description: 路线请求
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "路线请求")
public class RouteREQ implements Serializable {

    @Schema(description = "路线号")
    private Long id;

    @Schema(description = "路线名")
    private String routeName;

    @Schema(description = "起点站ID")
    private Long startStationId;

    @Schema(description = "终点站ID")
    private Long endStationId;

    @Schema(description = "站点数")
    private Integer stationCount;

}
