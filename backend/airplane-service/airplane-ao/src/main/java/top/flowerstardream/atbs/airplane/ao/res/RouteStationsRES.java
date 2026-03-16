package top.flowerstardream.atbs.airplane.ao.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.ao.res.BaseMgmtRES;

import java.io.Serializable;

/**
 * @Author: QAQ
 * @Date: 2025/11/09/23:00
 * @Description: 路线站点返回参数
 */

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "路线站点返回参数")
public class RouteStationsRES extends BaseMgmtRES {

    @Schema(description = "路线名")
    private String routeName;

    @Schema(description = "站点名")
    private String stationName;

    @Schema(description = "站点排序")
    private Integer stationSorting;


}
