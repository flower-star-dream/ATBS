package top.flowerstardream.atbs.airplane.ao.res;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.ao.res.BaseMgmtRES;

import java.io.Serializable;

/**
 * @Author: QAQ
 * @Date: 2025/11/09/23:00
 * @Description: 站点返回参数
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "站点返回参数")
public class StationMgmtRES extends BaseMgmtRES {

    @Schema(description = "站点名称")
    private String stationName;

    @Schema(description = "地址")
    private String address;

}
