package top.flowerstardream.atbs.order.ao.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.flowerstardream.atbs.order.common.enums.TicketStatus;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @Author: 花海
 * @Date: 2024/11/11
 * @Description: 机票响应对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "机票响应对象")
public class TicketRES implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 票ID
     */
    @Schema(description = "票ID")
    private Long id;

    /**
     * 班次号
     */
    @Schema(description = "班次号")
    private Long scheduleId;

    /**
     * 订单ID
     */
    @Schema(description = "订单ID")
    private Long orderId;

    /**
     * 乘车人姓名
     */
    @Schema(description = "乘车人姓名")
    private String realName;

    /**
     * 乘车人证件类型
     */
    @Schema(description = "乘车人证件类型")
    private String cardType;

    /**
     * 乘车人证件号
     */
    @Schema(description = "乘车人证件号")
    private String idCard;

    /**
     * 座位号
     */
    @Schema(description = "座位号")
    private Integer seatNumber;

    /**
     * 机票状态
     */
    @Schema(description = "机票状态")
    private TicketStatus status;

    /**
     * 票价
     */
    @Schema(description = "票价")
    private BigDecimal money;

    /**
     * 出发时间
     */
    @Schema(description = "出发时间")
    private LocalDateTime startTime;

    /**
     * 到达时间
     */
    @Schema(description = "到达时间")
    private LocalDateTime endTime;

    /**
     * 出发站id
     */
    @Schema(description = "出发站id")
    private Long startStationId;
    /**
     * 出发站
     */
    @Schema(description = "出发站")
    private String startStation;

    /**
     * 到达站id
     */
    @Schema(description = "到达站id")
    private Long endStationId;

    /**
     * 到达站
     */
    @Schema(description = "到达站")
    private String endStation;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @Schema(description = "创建人")
    private String createPerson;

    /**
     * 更新人
     */
    @Schema(description = "更新人")
    private String updatePerson;
}