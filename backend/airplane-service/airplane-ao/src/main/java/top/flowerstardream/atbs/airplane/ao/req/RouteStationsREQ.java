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
 * @Description: 路线站点请求
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "路线站点请求")
public class RouteStationsREQ implements Serializable {

    @Schema(description = "路线站点号")
    private Long id;

    @Schema(description = "路线号")
    private Long routeId;

    @Schema(description = "站点号")
    private Long stationId;

    @Schema(description = "站点排序")
    private Integer stationSorting;

    @Schema(description = "是否初始化")
    private Boolean init = false;
}
