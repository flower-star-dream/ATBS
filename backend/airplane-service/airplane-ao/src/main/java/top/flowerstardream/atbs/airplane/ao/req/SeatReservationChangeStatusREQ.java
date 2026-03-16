package top.flowerstardream.atbs.airplane.ao.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.ao.req.BaseStatusChangeREQ;
import top.flowerstardream.base.state.BaseStatus;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: 花海
 * @Date: 2025/12/02/00:06
 * @Description: 座位预订状态修改请求参数
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "座位预订状态修改请求参数")
public class SeatReservationChangeStatusREQ extends BaseStatusChangeREQ<BaseStatus> {
    @Schema(description = "座位预订ids")
    private List<Long> ids;
}
