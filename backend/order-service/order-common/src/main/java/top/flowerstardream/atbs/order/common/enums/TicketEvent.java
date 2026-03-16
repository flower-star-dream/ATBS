package top.flowerstardream.atbs.order.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.flowerstardream.base.state.IBaseEvent;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author: 花海
 * @Date: 2025/11/11/13:21
 * @Description: 票务事件枚举
 */
@AllArgsConstructor
@Getter
public enum TicketEvent implements IBaseEvent<String> {
    USE("USE", "使用"),
    CHANGE("CHANGE", "改签"),
    CANCEL("CANCEL", "取消"),
    REFUND_TICKET("REFUND_TICKET", "退票");

    private final String code;
    private final String name;

    private static final Map<String, TicketEvent> CODE_MAP = Arrays.stream(values())
        .collect(Collectors.toMap(TicketEvent::getCode, Function.identity()));

    public static TicketEvent fromCode(String code) {
        return CODE_MAP.get(code);
    }
}