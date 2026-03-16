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
@Schema(description = "站点分页查询请求")
public class StationPageQueryREQ extends BasePageQueryREQ {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "站点号")
    private Long id;

    @Schema(description = "站点名称")
    private String stationName;

    @Schema(description = "地址")
    private String address;

}
