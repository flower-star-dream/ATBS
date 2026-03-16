package top.flowerstardream.atbs.airplane.ao.res;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.ao.res.BaseMgmtRES;

import java.io.Serializable;

/**
 * @Author: QAQ
 * @Date: 2025/11/09/23:00
 * @Description: 飞机返回参数
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "飞机返回参数")
public class AirplaneRES extends BaseMgmtRES {

    @Schema(description = "飞机名称")
    private String airplaneName;

    @Schema(description = "飞机型号")
    private String airplaneModel;

    @Schema(description = "座位数")
    private Integer seatNum;

    @Schema(description = "服务年数")
    private Integer serviceYears;

}
