package top.flowerstardream.atbs.order.common.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import top.flowerstardream.base.state.IBaseState;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author: 花海
 * @Date: 2024/11/11
 * @Description: 机票状态枚举
 */
@Getter
@AllArgsConstructor
public enum TicketStatus implements IBaseState<Integer>, IEnum<Integer> {

    /**
     * 正常状态
     */
    NORMAL(1, "正常"),

    /**
     * 已使用
     */
    USED(2, "已使用"),

    /**
     * 已取消
     */
    CANCELLED(3, "已取消"),

    /**
     * 已改签
     */
    CHANGED(4, "已改签"),

    /**
     * 已退票
     */
    REFUNDED(5, "已退票");

    private final Integer code;
    private final String name;

    private static final Map<Integer, TicketStatus> CODE_MAP = Arrays.stream(values())
        .collect(Collectors.toMap(TicketStatus::getCode, Function.identity()));

    public static TicketStatus valueOf(Integer code) {
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