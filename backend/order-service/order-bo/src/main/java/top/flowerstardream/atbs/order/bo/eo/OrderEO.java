package top.flowerstardream.atbs.order.bo.eo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.atbs.order.common.enums.OrderStatus;
import top.flowerstardream.base.bo.eo.BizBaseEO;
import top.flowerstardream.base.state.StatusAble;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @Author: 花海
 * @Date: 2025/11/11/21:16
 * @Description: 订单实体对象
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("atbs_order")
public class OrderEO extends BizBaseEO<OrderStatus> implements StatusAble<OrderStatus> {

    // 用户id
    @TableField("user_id")
    private Long userId;

    // 订单备注
    @TableField("remarks")
    private String remarks;

    // 订单总价
    @TableField("total_price")
    private BigDecimal totalPrice;

    // 支付时间
    @TableField("pay_time")
    private LocalDateTime payTime;

    // 已付金额
    @TableField("amount_paid")
    private BigDecimal amountPaid;
}
