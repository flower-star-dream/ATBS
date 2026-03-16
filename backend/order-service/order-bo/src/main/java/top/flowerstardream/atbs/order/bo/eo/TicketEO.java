package top.flowerstardream.atbs.order.bo.eo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.atbs.order.common.enums.TicketStatus;
import top.flowerstardream.base.bo.eo.BizBaseEO;
import top.flowerstardream.base.state.StatusAble;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @Author: 花海
 * @Date: 2025/11/11/16:01
 * @Description: 机票实体对象
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("atbs_ticket")
public class TicketEO extends BizBaseEO<TicketStatus> implements StatusAble<TicketStatus> {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属订单号,; */
    @TableField("order_id")
    private Long orderId;

    /** 乘车人id,; */
    @TableField("passenger_id")
    private Long passengerId;

    /** 座位预订号,; */
    @TableField("seat_reservation_id")
    private Long seatReservationId;

    /** 票价,; */
    @TableField("money")
    private BigDecimal money;

    /** 出发时间,; */
    @TableField("start_time")
    private LocalDateTime startTime;

    /** 到达时间,; */
    @TableField("end_time")
    private LocalDateTime endTime;

    /** 出发站,; */
    @TableField("start_station_id")
    private Long startStationId;

    /** 到达站,; */
    @TableField("end_station_id")
    private Long endStationId;

}
