package top.flowerstardream.atbs.order.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.flowerstardream.base.state.IBaseEvent;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author JAM
 */
@AllArgsConstructor
@Getter
public enum OrderEvent implements IBaseEvent<String> {
    PAY("PAY", "支付"),
    ISSUE_TICKETS("ISSUE_TICKETS", "出票"),
    COMPLETE("COMPLETE", "完成"),
    CANCEL("CANCEL", "取消"),
    REFUND("REFUND", "退款"),
    MAKE_UP_THE_DIFFERENCE("MAKE_UP_THE_DIFFERENCE", "补差价");

    private final String code;
    private final String name;

    private static final Map<String, OrderEvent> CODE_MAP = Arrays.stream(values())
        .collect(Collectors.toMap(OrderEvent::getCode, Function.identity()));

    public static OrderEvent fromCode(String code) {
        return CODE_MAP.get(code);
    }
}