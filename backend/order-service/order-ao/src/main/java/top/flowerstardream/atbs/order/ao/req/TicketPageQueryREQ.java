package top.flowerstardream.atbs.order.ao.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import top.flowerstardream.base.ao.req.BasePageQueryREQ;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Author: 花海
 * @Date: 2024/11/11
 * @Description: 机票分页查询请求对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "机票分页查询请求对象")
public class TicketPageQueryREQ extends BasePageQueryREQ implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    @Schema(description = "订单ID")
    private Long orderId;

    /**
     * 班次ID
     */
    @Schema(description = "班次ID")
    private Long scheduleId;

    /**
     * 乘车人ID
     */
    @Schema(description = "乘车人姓名")
    private String passengerName;


    /**
     * 出发站
     */
    @Schema(description = "出发站")
    private String startStation;

    /**
     * 到达站
     */
    @Schema(description = "到达站")
    private String endStation;

    /**
     * 乘车日期范围-开始
     */
    @Schema(description = "乘车日期范围-开始")
    @JsonFormat(pattern = "yyyy-MM-ddTHH:mm:ss")
    private LocalDateTime rideDateStart;

    /**
     * 乘车日期范围-结束
     */
    @Schema(description = "乘车日期范围-结束")
    @JsonFormat(pattern = "yyyy-MM-ddTHH:mm:ss")
    private LocalDateTime rideDateEnd;
}