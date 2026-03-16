package top.flowerstardream.atbs.airplane.bo.eo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.bo.eo.AuditBaseEO;
import top.flowerstardream.base.bo.eo.BizBaseEO;
import top.flowerstardream.base.state.BaseStatus;
import top.flowerstardream.base.state.StatusAble;

import java.io.Serializable;

/**
 * @Author: 花海
 * @Date: 2025/11/06/19:00
 * @Description: 座位预定实体对象
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("atbs_seat_reservation")
public class SeatReservationEO extends BizBaseEO<BaseStatus> implements StatusAble<BaseStatus> {

    // 班次号
    @TableField("schedule_id")
    private Long scheduleId;

    // 座位号
    @TableField("seat_number")
    private Integer seatNum;

    @TableLogic(value = "0", delval = "1")
    @TableField("deleted")
    protected Boolean deleted;

    @Version
    @TableField(value = "version")
    protected Integer version;
}
