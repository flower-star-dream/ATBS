package top.flowerstardream.atbs.airplane.bo.eo;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.bo.eo.BaseEO;

import java.io.Serializable;

/**
 * @Author: 花海
 * @Date: 2025/11/06/19:00
 * @Description: 路线站点实体对象
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("atbs_route_stations")
public class RouteStationsEO extends BaseEO implements Serializable {

    // 路线id
    @TableField("route_id")
    private Long routeId;

    // 站点id
    @TableField("station_id")
    private Long stationId;

    // 站点排序
    @TableField("station_sorting")
    private Integer stationSorting;

}
