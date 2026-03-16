package top.flowerstardream.atbs.order.biz.state;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import top.flowerstardream.atbs.order.ao.req.OrderStatusREQ;
import top.flowerstardream.atbs.order.ao.req.TicketStatusChangeREQ;
import top.flowerstardream.atbs.order.biz.client.AirplaneClient;
import top.flowerstardream.atbs.order.biz.mapper.OrderMapper;
import top.flowerstardream.atbs.order.biz.mapper.TicketMapper;
import top.flowerstardream.atbs.order.biz.service.IOrderService;
import top.flowerstardream.atbs.order.biz.service.ITicketService;
import top.flowerstardream.atbs.order.bo.dto.CalcTicketPriceDTO;
import top.flowerstardream.atbs.order.bo.dto.CancelTicketDTO;
import top.flowerstardream.atbs.order.bo.dto.TicketDTO;
import top.flowerstardream.atbs.order.bo.eo.OrderEO;
import top.flowerstardream.atbs.order.bo.eo.TicketEO;
import top.flowerstardream.atbs.order.common.enums.OrderEvent;
import top.flowerstardream.atbs.order.common.enums.OrderStatus;
import top.flowerstardream.atbs.order.common.enums.TicketEvent;
import top.flowerstardream.atbs.order.common.enums.TicketStatus;
import top.flowerstardream.base.annotation.StateRouter;
import top.flowerstardream.base.state.IStateRouter;
import top.flowerstardream.base.utils.StateRouteParams;
import top.flowerstardream.base.utils.WeChatPayUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import static top.flowerstardream.atbs.order.common.enums.TicketEvent.*;
import static top.flowerstardream.atbs.order.common.enums.TicketStatus.*;


/**
 * @Author: 花海
 * @Date: 2026/03/06/13:11
 * @Description: 订单状态路由
 */
@Component
@StateRouter
public class TicketRouter extends ServiceImpl<TicketMapper, TicketEO> implements IStateRouter<TicketStatus, TicketEvent, TicketEO>{
    @Resource
    private IOrderService orderService;

    @Resource
    private WeChatPayUtil weChatPayUtil;

    @Resource
    private AirplaneClient airplaneClient;

    @Resource
    private ITicketService ticketService;

    @Resource
    @Lazy
    private IStateRouter<TicketStatus, TicketEvent, OrderEO> self;

    /**
     * 状态×事件 → 目标状态  配置表
     */
    private static final Map<TicketStatus, Map<TicketEvent, TicketStatus>> CONFIG = Map.of(
        NORMAL, Map.of(
                USE, USED,
                CHANGE, CHANGED,
                CANCEL, CANCELLED,
                REFUND_TICKET, REFUNDED
        )
    );

    /**
     * 事件 → 业务实现  路由表
     */
    private final Map<TicketEvent, Function<StateRouteParams, TicketStatus>> DISPATCHER =
            Map.of(
                USE,  this::use,
                CHANGE, this::change,
                CANCEL, this::cancel,
                REFUND_TICKET, this::refund
            );

    @Override
    public Map<TicketStatus, Map<TicketEvent, TicketStatus>> getStateEventTargetConfig() {
        return CONFIG;
    }

    @Override
    public Map<TicketEvent, Function<StateRouteParams, TicketStatus>> getEventDispatcher() {
        return DISPATCHER;
    }

    private TicketStatus refund(StateRouteParams params) {
        TicketEO oldTicket = params.getParam("oldTicket");
        if (oldTicket.getSeatReservationId() != null) {
            airplaneClient.releaseSeat(Collections.singletonList(oldTicket.getSeatReservationId()));
            orderService.orderRefund(oldTicket.getOrderId());
        }
        return common(params, REFUNDED);
    }

    private TicketStatus cancel(StateRouteParams params) {
        TicketEO oldTicket = params.getParam("oldTicket");
        if (oldTicket.getSeatReservationId() != null) {
            airplaneClient.releaseSeat(Collections.singletonList(oldTicket.getSeatReservationId()));
        }
        return common(params, CANCELLED);
    }

    private TicketStatus change(StateRouteParams params) {
        TicketEO oldTicket = params.getParam("oldTicket");
        TicketStatusChangeREQ req = params.getParam("req");
        if (oldTicket.getSeatReservationId() != null) {
            airplaneClient.releaseSeat(Collections.singletonList(oldTicket.getSeatReservationId()));
            TicketDTO ticketDTO = TicketDTO.builder()
                    .scheduleId(req.getScheduleId())
                    .orderId(oldTicket.getOrderId())
                    .passengerIds(Collections.singletonList(oldTicket.getPassengerId()))
                    .startStationId(req.getStartStationId())
                    .endStationId(req.getEndStationId())
                    .build();
            CalcTicketPriceDTO calcTicketPriceDTO = CalcTicketPriceDTO.builder()
                    .scheduleId(req.getScheduleId())
                    .startStationId(req.getStartStationId())
                    .endStationId(req.getEndStationId())
                    .build();
            BigDecimal newPrice = airplaneClient.calcTicketPrice(calcTicketPriceDTO).getData();
            ticketDTO.setMoney(newPrice);
            ticketService.createTickets(ticketDTO);
            BigDecimal newTotalPrice = newPrice.subtract(oldTicket.getMoney());
            orderService.updateTotalPrice(oldTicket.getOrderId(), newTotalPrice);
        }
        return common(params, CHANGED);
    }

    private TicketStatus use(StateRouteParams params) {
        return common(params, USED);
    }

    private TicketStatus common(StateRouteParams params, TicketStatus status) {
        if (!params.contains("oldTicket")) {
            return null;
        }
        TicketEO oldTicket = params.getParam("oldTicket");
        TicketEO ticketEO = TicketEO.builder()
                .id(oldTicket.getId())
                .status(status)
                .build();
        return ticketEO.getStatus();
    }

}
