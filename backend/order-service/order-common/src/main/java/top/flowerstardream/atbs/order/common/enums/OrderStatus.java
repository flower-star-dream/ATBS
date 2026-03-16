package top.flowerstardream.atbs.order.common.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import top.flowerstardream.base.state.BaseStatus;
import top.flowerstardream.base.state.IBaseEvent;
import top.flowerstardream.base.state.IBaseState;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static top.flowerstardream.atbs.order.common.enums.OrderExceptionEnum.ORDER_STATUS_INVALID;

/**
 * @Author: 花海
 * @Date: 2026/03/06/12:07
 * @Description: 订单状态枚举
 */
@AllArgsConstructor
@Getter
public enum OrderStatus implements IBaseState<Integer>, IEnum<Integer> {
    PENDING_PAY(0, "待支付"),
    PAID(1, "已支付"),
    TICKETED(2, "已出票"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消"),
    REFUNDED(5, "已退款");

    private final Integer code;
    private final String name;

    private static final Map<Integer, OrderStatus> CODE_MAP = Arrays.stream(values())
        .collect(Collectors.toMap(OrderStatus::getCode, Function.identity()));

    public static OrderStatus valueOf(Integer code) {
        return CODE_MAP.get(code);
    }

    /**
     * 获取状态码
     * @return 状态码
     */
    @Override
    public Integer getValue() {
        return this.code;
    }
}