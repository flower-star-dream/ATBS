package top.flowerstardream.atbs.airplane.ao.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.ao.res.BaseMgmtRES;

import java.io.Serializable;

/**
 * @Author: QAQ
 * @Date: 2025/11/09/23:00
 * @Description: 路线返回参数
 */

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "路线返回参数")
public class RouteRES extends BaseMgmtRES {

    @Schema(description = "路线名")
    private String routeName;

    @Schema(description = "起始站")
    private String startStationName;

    @Schema(description = "终点站")
    private String endStationName;

    @Schema(description = "站点数")
    private Integer stationCount;

}
