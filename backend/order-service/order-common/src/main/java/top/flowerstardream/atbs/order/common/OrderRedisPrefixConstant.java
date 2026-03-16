package top.flowerstardream.atbs.order.common;

/**
 * @Author: 花海
 * @Date: 2024/11/11
 * @Description: 订单Redis前缀常量
 */
public interface OrderRedisPrefixConstant {
    /**
     * 订单防重锁前缀
     */
    String ORDER_REPEAT_PREFIX = "lock:order:repeat:";

}