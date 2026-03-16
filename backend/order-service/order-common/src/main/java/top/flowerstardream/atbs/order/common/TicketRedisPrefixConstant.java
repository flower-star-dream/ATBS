package top.flowerstardream.atbs.order.common;

/**
 * @Author: 花海
 * @Date: 2024/11/11
 * @Description: 机票Redis前缀常量
 */
public interface TicketRedisPrefixConstant {

    /**
     * 机票缓存前缀
     */
    String TICKET_CACHE_PREFIX = "ticket:info:";

    /**
     * 订单机票列表缓存前缀
     */
    String ORDER_TICKET_LIST_PREFIX = "order:ticket:list:";

    /**
     * 用户机票列表缓存前缀
     */
    String USER_TICKET_LIST_PREFIX = "user:ticket:list:";

    /**
     * 余票缓存前缀
     */
    String REMAINING_TICKET_PREFIX = "ticket:remaining:";

    /**
     * 座位锁定前缀
     */
    String SEAT_LOCK_PREFIX = "seat:lock:";

    /**
     * 机票状态变更锁前缀
     */
    String TICKET_STATUS_LOCK_PREFIX = "ticket:state:lock:";
}