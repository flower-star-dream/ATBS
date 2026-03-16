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
import top.flowerstardream.base.bo.eo.BaseEO;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Author: 花海
 * @Date: 2025/11/06/19:00
 * @Description: 航班实体对象
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("atbs_schedule")
public class ScheduleEO extends AuditBaseEO {

    // 飞机号
    @TableField("airplane_id")
    private Long airplaneId;

    // 路线号
    @TableField("route_id")
    private Long routeId;

    // 飞机长
    @TableField("captain")
    private String captain;

    // 余票
    @TableField("available_tickets")
    private Integer availableTickets;

    // 始发站
    @TableField("start_time")
    private LocalDateTime startTime;

    // 终点站
    @TableField("end_time")
    private LocalDateTime endTime;

    @TableLogic(value = "0", delval = "1")
    @TableField("deleted")
    protected Boolean deleted;

    @Version
    @TableField(value = "version")
    protected Integer version;
}
