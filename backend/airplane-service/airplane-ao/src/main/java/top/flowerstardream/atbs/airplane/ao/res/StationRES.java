package top.flowerstardream.atbs.airplane.ao.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;

/**
 * @Author: QAQ
 * @Date: 2025/11/09/23:00
 * @Description: 站点返回参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "站点返回参数")
public class StationRES implements Serializable {

    @Schema(description = "站点号")
    private Long id;

    @Schema(description = "站点名称")
    private String stationName;

    @Schema(description = "地址")
    private String address;

}
