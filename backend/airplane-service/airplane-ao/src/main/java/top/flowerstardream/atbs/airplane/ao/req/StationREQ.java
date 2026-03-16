package top.flowerstardream.atbs.airplane.ao.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author: QAQ
 * @Date: 2025/11/09/23:00
 * @Description: 站点请求
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "站点请求")
public class StationREQ implements Serializable {

    @Schema(description = "站点号")
    private Long id;

    @Schema(description = "站点名称")
    private String stationName;

    @Schema(description = "地址")
    private String address;

}
