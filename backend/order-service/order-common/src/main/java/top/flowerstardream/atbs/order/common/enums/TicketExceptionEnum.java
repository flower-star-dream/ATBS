package top.flowerstardream.atbs.order.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.flowerstardream.base.exception.IExceptionEnum;

/**
 * @Author: 花海
 * @Date: 2025/11/11
 * @Description: 机票异常枚举
 */
@Getter
@AllArgsConstructor
public enum TicketExceptionEnum implements IExceptionEnum {

    /**
     * 机票不存在
     */
    TICKET_NOT_EXIST(50001, "机票不存在"),

    /**
     * 机票已被使用
     */
    TICKET_ALREADY_USED(50002, "机票已被使用"),

    /**
     * 机票已被取消
     */
    TICKET_ALREADY_CANCELLED(50003, "机票已被取消"),

    /**
     * 无权操作此机票
     */
    TICKET_PERMISSION_DENIED(50004, "无权操作此机票"),

    /**
     * 余票不足
     */
    TICKET_INSUFFICIENT(50005, "余票不足"),

    /**
     * 座位预订失败
     */
    SEAT_RESERVATION_FAILED(50006, "座位预订失败"),

    /**
     * 退票时间已过
     */
    REFUND_TIME_EXPIRED(50007, "退票时间已过"),

    /**
     * 改签时间已过
     */
    CHANGE_TIME_EXPIRED(50008, "改签时间已过"),
    /**
     * 机票当前状态不允许此操作
     */
    TICKET_STATUS_NOT_ALLOWED(50009, "机票当前状态不允许此操作"),
    /**
     * 订单当前状态不允许操作此机票
     */
    ORDER_STATUS_NOT_ALLOWED(50010, "订单当前状态不允许操作此机票");

    private final Integer code;
    private final String message;

    public static TicketExceptionEnum valueOf(Integer code) {
        for (TicketExceptionEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}