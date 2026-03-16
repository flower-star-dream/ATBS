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
 * @Description: 飞机请求
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "飞机请求")
public class AirplaneREQ implements Serializable {

    @Schema(description = "飞机号")
    private Long id;

    @Schema(description = "飞机名称")
    private String airplaneName;

    @Schema(description = "飞机型号")
    private String airplaneModel;

    @Schema(description = "座位数")
    private Integer seatNum;

    @Schema(description = "服务年数")
    private Integer serviceYears;

}
