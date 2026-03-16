package top.flowerstardream.atbs.order.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.flowerstardream.base.exception.IExceptionEnum;

/**
 * @Author: 花海
 * @Date: 2025/11/11/12:05
 * @Description: 订单异常枚举
 */
@Getter
@AllArgsConstructor
public enum OrderExceptionEnum implements IExceptionEnum {

    /**
     * 订单未找到
     */
    ORDER_NOT_FOUND(40001, "订单不存在"),

    /**
     * 订单创建失败
     */
    ORDER_CREATE_FAILED(40002, "订单创建失败"),

    /**
     * 订单更新失败
     */
    ORDER_UPDATE_FAILED(40003, "订单更新失败"),

    /**
     * 订单取消失败
     */
    ORDER_CANCEL_FAILED(40004, "订单取消失败"),

    /**
     * 订单状态无效
     */
    ORDER_STATUS_INVALID(40005, "订单状态无效"),

    /**
     * 订单状态错误
     */
    ORDER_STATUS_ERROR(40006, "订单状态错误"),

    /**
     * 订单权限不足
     */
    ORDER_PERMISSION_DENIED(40007, "无权访问此订单"),

    /**
     * 不能重复创建订单
     */
    ORDER_REPEAT_CREATE(40008, "不能重复创建订单"),
    /**
     * 该订单已支付
     */
    ORDER_ALREADY_PAID(40009, "该订单已支付"),
    /**
     * 订单退款失败
     */
    ORDER_REFUND_FAILED(40010, "订单退款失败"),
    /**
     * 订单当前状态禁止退款
     */
    ORDER_REFUND_FORBIDDEN(40011, "当前订单状态禁止退款");


    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误信息
     */
    private final String message;

    public static OrderExceptionEnum valueOf(Integer code) {
        for (OrderExceptionEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}