package top.flowerstardream.atbs.airplane.ao.pqreq;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.ao.req.BasePageQueryREQ;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author: QAQ
 * @Date: 2025/11/11/15：00
 * @Description: 路线分页查询请求
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "路线分页查询请求")
public class RoutePageQueryREQ extends BasePageQueryREQ {
    @Serial
    private static final long serialVersionUID = 1L;

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
