package top.flowerstardream.atbs.airplane.ao.pqreq;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.ao.req.BasePageQueryREQ;

import java.io.Serial;
import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "飞机分页查询请求")
public class AirplanePageQueryREQ extends BasePageQueryREQ {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "飞机号")
    private Long id;

    @Schema(description = "飞机名")
    private String airplaneName;

    @Schema(description = "飞机型号")
    private String airplaneModel;

    @Schema(description = "服务年数")
    private Integer serviceYears;
}
