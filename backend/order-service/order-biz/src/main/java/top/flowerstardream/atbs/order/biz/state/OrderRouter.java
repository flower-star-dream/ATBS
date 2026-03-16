package top.flowerstardream.atbs.order.biz.state;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import top.flowerstardream.atbs.order.ao.req.OrderStatusREQ;
import top.flowerstardream.atbs.order.biz.mapper.OrderMapper;
import top.flowerstardream.atbs.order.biz.service.ITicketService;
import top.flowerstardream.atbs.order.bo.dto.CancelTicketDTO;
import top.flowerstardream.atbs.order.bo.eo.OrderEO;
import top.flowerstardream.atbs.order.common.enums.OrderEvent;
import top.flowerstardream.atbs.order.common.enums.OrderStatus;
import top.flowerstardream.base.annotation.StateRouter;
import top.flowerstardream.base.bo.dto.BaseStatusDTO;
import top.flowerstardream.base.state.BaseEvent;
import top.flowerstardream.base.state.BaseStatus;
import top.flowerstardream.base.state.IStateRouter;
import top.flowerstardream.base.utils.StateRouteParams;
import top.flowerstardream.base.utils.WeChatPayUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;

import static top.flowerstardream.atbs.order.common.enums.OrderEvent.*;
import static top.flowerstardream.atbs.order.common.enums.OrderExceptionEnum.*;
import static top.flowerstardream.atbs.order.common.enums.OrderStatus.*;
import static top.flowerstardream.base.exception.BaseExceptionEnum.PARAM_ERROR;
import static top.flowerstardream.base.exception.ExceptionEnum.MODIFICATION_FAILED;

/**
 * @Author: 花海
 * @Date: 2026/03/06/13:11
 * @Description: 订单状态路由
 */
@Component
@StateRouter
public class OrderRouter extends ServiceImpl<OrderMapper, OrderEO> implements IStateRouter<OrderStatus, OrderEvent, OrderEO> {

    @Resource
    private ITicketService ticketService;

    @Resource
    private WeChatPayUtil weChatPayUtil;

    @Resource
    @Lazy
    private IStateRouter<OrderStatus, OrderEvent, OrderEO> self;

    /**
     * 状态×事件 → 目标状态  配置表
     */
    private static final Map<OrderStatus, Map<OrderEvent, OrderStatus>> CONFIG = Map.of(
        PENDING_PAY, Map.of(
                PAY, PAID,
                CANCEL, CANCELLED
        ),
        PAID, Map.of(
                ISSUE_TICKETS, TICKETED,
                MAKE_UP_THE_DIFFERENCE, PENDING_PAY,
                REFUND, REFUNDED
        ),
        TICKETED, Map.of(
                COMPLETE, COMPLETED,
                MAKE_UP_THE_DIFFERENCE, PENDING_PAY,
                REFUND, REFUNDED
        )
    );

    /**
     * 事件 → 业务实现  路由表
     */
    private final Map<OrderEvent, Function<StateRouteParams, OrderStatus>> DISPATCHER =
            Map.of(
                PAY,  this::pay,
                ISSUE_TICKETS, this::issueTickets,
                COMPLETE, this::complete,
                CANCEL, this::cancel,
                REFUND, this::refund,
                MAKE_UP_THE_DIFFERENCE, this::makeUpTheDifference
            );


    @Override
    public Map<OrderStatus, Map<OrderEvent, OrderStatus>> getStateEventTargetConfig() {
        return CONFIG;
    }

    @Override
    public Map<OrderEvent, Function<StateRouteParams, OrderStatus>> getEventDispatcher() {
        return DISPATCHER;
    }

    private OrderStatus pay(StateRouteParams param) {
        OrderStatus res = common(param, PAID);
        if (res != null) {
            return res;
        }
        OrderEO orderEO = param.getParam("orderEO");
        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        OrderEO orders = OrderEO.builder()
                .id(orderEO.getId())
                .status(PAID)
                .payTime(LocalDateTime.now())
                .amountPaid(orderEO.getTotalPrice())
                .build();
        if (!self.updateById(orders)) {
            throw ORDER_UPDATE_FAILED.toException();
        }
        return orders.getStatus();
    }
    private OrderStatus issueTickets(StateRouteParams param) {
        return common(param, TICKETED);
    }
    private OrderStatus complete(StateRouteParams param) {
        return common(param, COMPLETED);
    }
    private OrderStatus cancel(StateRouteParams param) {
        OrderStatus res = common(param, CANCELLED);
        if (res != null) {
            return res;
        }
        OrderEO orderEO = param.getParam("orderEO");
        CancelTicketDTO cancelTicketDTO = CancelTicketDTO.builder()
                .orderId(orderEO.getId())
                .status(CANCELLED)
                .build();
        ticketService.cancelTicketByOrder(cancelTicketDTO);
        if (!self.updateById(orderEO)) {
            throw ORDER_CANCEL_FAILED.toException();
        }
        return orderEO.getStatus();
    }

    // 退款总金额必须是原下单金额，此处为简略实现，实际运行时因为没有原金额字段在改签后微信方会报错
    public OrderStatus refund(StateRouteParams param){
        OrderEO order;
        OrderEO oldOrder;
        if (param.contains("req")) {
            OrderStatusREQ orderStatusREQ = param.getParam("req");
            oldOrder = self.getById(orderStatusREQ.getId());
            order = OrderEO.builder()
                    .id(orderStatusREQ.getId())
                    .status(REFUNDED)
                    .build();
            refundCore(order, oldOrder, oldOrder.getAmountPaid(), oldOrder.getTotalPrice());
        }
        oldOrder = param.getParam("orderEO");
        BigDecimal originalOrderMoney = oldOrder.getTotalPrice();
        order = OrderEO.builder()
                .id(oldOrder.getId())
                .status(REFUNDED)
                .build();
        BigDecimal refundPrice = param.getParam("newTotalPrice");
        if (refundPrice == null) {
            refundPrice = oldOrder.getAmountPaid();
        } else {
            order.setTotalPrice(order.getTotalPrice().add(refundPrice));
        }
        refundCore(order, oldOrder, refundPrice.abs(), originalOrderMoney);
        return order.getStatus();
    }

    private OrderStatus makeUpTheDifference(StateRouteParams param) {
        OrderEO orderEO = param.getParam("orderEO");
        BigDecimal newTotalPrice = param.getParam("newTotalPrice");
        orderEO.setStatus(PENDING_PAY);
        orderEO.setTotalPrice(orderEO.getTotalPrice().add(newTotalPrice));
        if (!self.updateById(orderEO)) {
            throw ORDER_UPDATE_FAILED.toException();
        }
        return orderEO.getStatus();
    }

    // 退款本应有退款表方便生成不同的退款单号，此处省略
    private void refundCore(OrderEO order, OrderEO oldOrder, BigDecimal refundPrice, BigDecimal originalOrderMoney){
        // 订单处于已支付和已出票状态下取消，需要进行退款
        if (oldOrder.getStatus().equals(PAID) && oldOrder.getStatus().equals(OrderStatus.TICKETED)) {
            //调用微信支付退款接口
//            weChatPayUtil.refund(
//                    oldOrder.getId().toString(),
//                    oldOrder.getId().toString() + refundPrice,
//                    refundPrice,
//                    originalOrderMoney);
            order.setAmountPaid(originalOrderMoney.subtract(refundPrice));
        }
        if (!self.updateById(order)) {
            throw ORDER_REFUND_FAILED.toException();
        }
    }

    private OrderStatus common(StateRouteParams param, OrderStatus status) {
        if (!param.contains("req")) {
            return null;
        }
        OrderStatusREQ orderStatusREQ = param.getParam("req");
        OrderEO orderEO = OrderEO.builder()
                .id(orderStatusREQ.getId())
                .status(status)
                .build();
        if (!self.updateById(orderEO)) {
            throw ORDER_UPDATE_FAILED.toException();
        }
        return orderEO.getStatus();
    }

}
