package top.flowerstardream.atbs.order.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.flowerstardream.atbs.order.ao.req.OrderPageQueryREQ;
import top.flowerstardream.atbs.order.ao.req.OrderREQ;
import top.flowerstardream.atbs.order.ao.req.OrderStatusREQ;
import top.flowerstardream.atbs.order.ao.req.OrdersPaymentREQ;
import top.flowerstardream.atbs.order.ao.res.OrderMgmtRES;
import top.flowerstardream.atbs.order.ao.res.OrderPaymentRES;
import top.flowerstardream.atbs.order.ao.res.OrderRES;
import top.flowerstardream.atbs.order.biz.client.AirplaneClient;
import top.flowerstardream.atbs.order.biz.client.UserClient;
import top.flowerstardream.atbs.order.biz.mapper.OrderMapper;
import top.flowerstardream.atbs.order.biz.service.IOrderService;
import top.flowerstardream.atbs.order.biz.service.ITicketService;
import top.flowerstardream.atbs.order.bo.dto.CalcTicketPriceDTO;
import top.flowerstardream.atbs.order.bo.dto.CancelTicketDTO;
import top.flowerstardream.atbs.order.bo.dto.TicketDTO;
import top.flowerstardream.atbs.order.bo.dto.UserDTO;
import top.flowerstardream.atbs.order.bo.eo.OrderEO;
import top.flowerstardream.atbs.order.common.enums.OrderEvent;
import top.flowerstardream.atbs.order.common.enums.OrderStatus;
import top.flowerstardream.base.annotation.AutoStateMachine;
import top.flowerstardream.base.ao.res.BaseStatusRES;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.state.BaseEvent;
import top.flowerstardream.base.state.BaseStatus;
import top.flowerstardream.base.state.StateMachine;
import top.flowerstardream.base.utils.RedisUtils;
import top.flowerstardream.base.utils.StateRouteParams;
import top.flowerstardream.base.utils.WeChatPayUtil;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static top.flowerstardream.atbs.order.common.OrderRedisPrefixConstant.*;
import static top.flowerstardream.atbs.order.common.enums.OrderEvent.*;
import static top.flowerstardream.atbs.order.common.enums.OrderExceptionEnum.*;
import static top.flowerstardream.atbs.order.common.enums.OrderStatus.*;
import static top.flowerstardream.base.exception.BaseExceptionEnum.*;
import static top.flowerstardream.base.utils.GetInfoUtil.*;

/**
 * @Author: 花海
 * @Date: 2025/11/11
 * @Description: 订单服务实现类
 */
@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEO> implements IOrderService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private ITicketService ticketService;

    @Resource
    private UserClient userClient;

    @Resource
    private AirplaneClient airplaneClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisUtils redisUtils;

//    @Resource
//    private WeChatPayUtil weChatPayUtil;

    @AutoStateMachine
    private StateMachine<OrderStatus, OrderEvent, OrderEO> fsm;

    @Resource
    @Lazy
    private IOrderService self;

    @Override
    public void createOrder(OrderREQ req) {
        Long userId = getOperatorId();
        // 参数校验
        if (req == null || userId == null || userId <= 0) {
            throw ORDER_PERMISSION_DENIED.toException();
        }
        // 1. 计算票价
        CalcTicketPriceDTO calcTicketPriceDTO = CalcTicketPriceDTO.builder()
                .scheduleId(req.getScheduleId())
                .startStationId(req.getStartStationId())
                .endStationId(req.getEndStationId())
                .build();
        // 向航空服务请求计算机票价格
        BigDecimal price = airplaneClient.calcTicketPrice(calcTicketPriceDTO).getData();
        BigDecimal totalPrice = price.multiply(new BigDecimal(req.getPassengerIds().size()));

        // 2. 幂等性校验, 10s内视为同一订单，不能重复创建订单
        String lockKey = ORDER_REPEAT_PREFIX + userId;
        Boolean firstAccess = redisUtils.execute("setIfAbsent", () -> stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(10)));
        if (Boolean.TRUE.equals(firstAccess)) {
            throw ORDER_REPEAT_CREATE.toException();
        }

        // 3. 创建订单
        LocalDateTime createTime = LocalDateTime.now();
        OrderEO orderEO = OrderEO.builder()
                        .userId(userId)
                        .status(PENDING_PAY)
                        .totalPrice(totalPrice)
                        .remarks(req.getRemarks())
                        .createTime(createTime)
                        .updateTime(createTime)
                        .build();
        // 4. 创建机票
        TicketDTO ticketDTO = new TicketDTO();
        BeanUtils.copyProperties(req, ticketDTO);
        ticketDTO.setMoney(price);
        createOrderAndTicket(ticketDTO, orderEO);
    }

    @GlobalTransactional(rollbackFor = Exception.class)
    private void createOrderAndTicket(TicketDTO ticketDTO, OrderEO orderEO) {
        if (!self.save(orderEO)) {
            throw ORDER_CREATE_FAILED.toException();
        }
        ticketDTO.setOrderId(orderEO.getId());
        // 向票务服务请求新增机票
        ticketService.createTickets(ticketDTO);
    }

    @Override
    public OrderMgmtRES getOrderById(Long id) {
        // 参数校验
        if (id == null || id <= 0) {
            throw ORDER_PERMISSION_DENIED.toException();
        }

        // 查询订单
        OrderEO orderEO = getById(id);
        if (orderEO == null) {
            throw ORDER_NOT_FOUND.toException();
        }
        OrderMgmtRES orderMgmtRES = new OrderMgmtRES();
        BeanUtils.copyProperties(orderEO, orderMgmtRES);
        orderMgmtRES.setUsername(userClient.getUserByIds(Collections.singletonList(orderEO.getUserId())).getData().get(0).getUsername());
        return orderMgmtRES;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderStatus(OrderStatusREQ req){
        // 参数校验
        if (req == null || req.getId() == null || req.getId() <= 0 || req.getStatus() == null) {
            throw PARAM_ERROR.toException();
        }

        // 查询订单是否存在
        OrderEO orderEO = self.getById(req.getId());
        if (orderEO == null) {
            throw ORDER_NOT_FOUND.toException();
        }
        // 更新订单状态
        if (Objects.equals(orderEO.getStatus(), COMPLETED) ||
                Objects.equals(orderEO.getStatus(), REFUNDED) ||
                Objects.equals(orderEO.getStatus(), CANCELLED)) {
            throw ORDER_STATUS_INVALID.toException();
        }

        if (StrUtil.isNotBlank(req.getRemarks())) {
            orderEO.setRemarks(req.getRemarks());
        }
        StateRouteParams params = StateRouteParams.create().addParam("req", req);
        switch (req.getStatus()) {
            case PAID -> fsm.fire(orderEO.getStatus(), PAY, params);
            case TICKETED -> fsm.fire(orderEO.getStatus(), ISSUE_TICKETS, params);
            case COMPLETED -> fsm.fire(orderEO.getStatus(), COMPLETE, params);
            case CANCELLED -> fsm.fire(orderEO.getStatus(), CANCEL, params);
            case REFUNDED -> fsm.fire(orderEO.getStatus(), REFUND, params);
        };
    }

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long userId){
        // 参数校验
        if (orderId == null || userId == null || userId <= 0) {
            throw ORDER_PERMISSION_DENIED.toException();
        }

        // 查询订单
        OrderEO orderEO = self.getById(orderId);
        if (orderEO == null) {
            throw ORDER_NOT_FOUND.toException();
        }

        // 验证订单归属
        if (!orderEO.getUserId().equals(userId)) {
            throw ORDER_PERMISSION_DENIED.toException();
        }

        // 检查订单状态是否可以取消
        if (Objects.equals(orderEO.getStatus(), COMPLETED) ||
                Objects.equals(orderEO.getStatus(), CANCELLED) ||
                Objects.equals(orderEO.getStatus(), REFUNDED)) {
            throw ORDER_STATUS_INVALID.toException();
        }

        StateRouteParams params = StateRouteParams.create().addParam("orderEO", orderEO);
        OrderStatus fire;
        // 订单状态处于已支付或已出票时，调用服务取消机票并退款
        if (Objects.equals(orderEO.getStatus(), PAID) ||
                Objects.equals(orderEO.getStatus(), TICKETED)) {
            fire = fsm.fire(orderEO.getStatus(), REFUND, params);
        } else {
            fire = fsm.fire(orderEO.getStatus(), CANCEL, params);
        }
        if (fire == null) {
            throw ORDER_CANCEL_FAILED.toException();
        }
    }

    /**
     * 后管分页查询
     * @param req 查询条件
     * @return
     */
    @Override
    public PageResult<OrderMgmtRES> pageQuery(OrderPageQueryREQ req) {
        // 参数校验
        if (req == null) {
            throw ORDER_PERMISSION_DENIED.toException();
        }

        // 构建查询条件
        if (req.getPage() <= 0) {
            req.setPage(1);
        }
        if (req.getPageSize() <= 0) {
            req.setPageSize(10);
        }
        Page<OrderEO> page = new Page<>(req.getPage(), req.getPageSize());
        // 这里可以根据req中的条件构建查询
        LambdaQueryWrapper<OrderEO> queryWrapper = Wrappers.lambdaQuery();
        if (req.getUserId() != null) {
            queryWrapper.eq(OrderEO::getUserId, req.getUserId());
        }
        if (StrUtil.isNotBlank(req.getUsername())) {
            List<Long> userIds = userClient.getUserIdsByName(req.getUsername()).getData();
            if (CollUtil.isNotEmpty(userIds)) {
                queryWrapper.in(OrderEO::getUserId, userIds);
            } else {
                queryWrapper.eq(OrderEO::getUserId, Optional.of(-1));
            }
        }
        if (req.getId() != null) {
            queryWrapper.eq(OrderEO::getId, req.getId());
        }

        IPage<OrderEO> orderPage = orderMapper.selectPage(page, queryWrapper);
        // 封装返回结果
        PageResult<OrderMgmtRES> pageResult = new PageResult<>();
        pageResult.setTotal(orderPage.getTotal());
        pageResult.setRecords(convertToRES(orderPage.getRecords()));
        return pageResult;
    }

    @Override
    public List<OrderRES> getUserOrders(Long userId) {
        // 参数校验
        if (userId == null || userId <= 0) {
            throw ORDER_PERMISSION_DENIED.toException();
        }

        // 构建查询条件
        LambdaQueryWrapper<OrderEO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(OrderEO::getUserId, userId);

        // 查询用户的所有订单
        List<OrderEO> orderList = orderMapper.selectList(queryWrapper);

        // 转换为OrderRES列表
        return orderList.stream().map(orderEO -> {
            OrderRES orderRES = new OrderRES();
            BeanUtils.copyProperties(orderEO, orderRES);
            return orderRES;
        }).toList();
    }

    /**
     * 更新订单总价
     *
     * @param orderId       订单ID
     * @param newTotalPrice 新的总价
     */
    @Override
    public void updateTotalPrice(Long orderId, BigDecimal newTotalPrice) {
        if (orderId == null || newTotalPrice == null) {
            throw ORDER_PERMISSION_DENIED.toException();
        }
        OrderEO orderEO = self.getById(orderId);
        if (orderEO == null) {
            throw ORDER_NOT_FOUND.toException();
        }
        StateRouteParams params = StateRouteParams
                    .create()
                    .addParam("orderEO", orderEO)
                    .addParam("newTotalPrice", newTotalPrice);
        if (newTotalPrice.compareTo(BigDecimal.ZERO) > 0) {
            log.info("订单 {} 需要补差价: {}元", orderId, newTotalPrice);
            fsm.fire(orderEO.getStatus(), MAKE_UP_THE_DIFFERENCE, params);
        } else if (newTotalPrice.compareTo(BigDecimal.ZERO) < 0) {
            fsm.fire(orderEO.getStatus(), REFUND, params);
        }
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentREQ
     * @return
     */
    @Override
    public OrderPaymentRES payment(OrdersPaymentREQ ordersPaymentREQ) {
        if (ordersPaymentREQ == null) {
            throw PARAM_ERROR.toException();
        }
        // 当前登录用户id
        Long userId = getOperatorId();
        String openid = userClient.getUserByIds(Collections.singletonList(userId)).getData().get(0).getOpenId();
        OrderEO orderEO = self.getById(ordersPaymentREQ.getOrderId());
        BigDecimal paymentAmount = orderEO.getTotalPrice().subtract(orderEO.getAmountPaid());
        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw ORDER_ALREADY_PAID.toException();
        }

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                String.valueOf(ordersPaymentREQ.getOrderId()), //商户订单号
//                paymentAmount, //支付金额，单位 元
//                "机票订单", //商品描述
//                openid //微信用户的openid
//        );
//
//        if ("ORDERPAID".equals(jsonObject.getStr("code"))) {
//            throw ORDER_ALREADY_PAID.toException();
//        }
//
//        OrderPaymentRES res = jsonObject.toBean(OrderPaymentRES.class);
//        res.setPackageStr(jsonObject.getStr("package"));

        // 用于测试，直接跳过预支付，交易单生成
        OrderPaymentRES res = new OrderPaymentRES();
        StateRouteParams params = StateRouteParams
                .create()
                .addParam("orderEO", orderEO);
        fsm.fire(orderEO.getStatus(), PAY, params);
        return res;
    }

    /**
     * 订单退款
     *
     * @param orderId
     */
    @Override
    public void orderRefund(Long orderId){
        if (orderId == null) {
            throw PARAM_ERROR.toException();
        }
        OrderEO orderEO = self.getById(orderId);
        if (orderEO == null) {
            throw ORDER_NOT_FOUND.toException();
        }
        if (!Objects.equals(orderEO.getStatus(), PAID) && !Objects.equals(orderEO.getStatus(), TICKETED)) {
            throw ORDER_REFUND_FORBIDDEN.toException();
        }
        StateRouteParams params = StateRouteParams
                .create()
                .addParam("orderEO", orderEO);
        fsm.fire(orderEO.getStatus(), REFUND, params);
    }

    /**
     * 获取订单状态
     *
     * @param orderId
     * @return
     */
    @Override
    public OrderStatus getOrderStatus(Long orderId) {
        if (orderId == null) {
            throw PARAM_ERROR.toException();
        }
        OrderEO orderEO = self.getById(orderId);
        if (orderEO == null) {
            throw ORDER_NOT_FOUND.toException();
        }
        return orderEO.getStatus();
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    @Override
    public void paySuccess(String outTradeNo, BigDecimal amount) {
        // 根据订单号查询订单
        OrderEO orderEO = self.getById(Long.valueOf(outTradeNo));
        StateRouteParams params = StateRouteParams
                .create()
                .addParam("orderEO", orderEO);
        fsm.fire(orderEO.getStatus(), PAY, params);
    }

    private List<OrderMgmtRES> convertToRES(List<OrderEO> orderList) {
        if (CollUtil.isEmpty(orderList)) {
            return Collections.emptyList();
        }

        /* 1. 查所有关联数据 */
        // 1.1 用户
        List<Long> userIds = orderList.stream()
                                            .map(OrderEO::getUserId)
                                            .distinct()
                                            .toList();
        Map<Long, UserDTO> userMap = userClient.getUserByIds(userIds)
                                                         .getData()
                                                         .stream()
                                                         .collect(Collectors.toMap(UserDTO::getId, Function.identity()));

        /* 2. 组装结果 */
        return orderList.stream()
                         .map(order -> {
                             OrderMgmtRES res = new OrderMgmtRES();
                             BeanUtils.copyProperties(order, res);

                             // 2.1 用户
                             UserDTO user = userMap.get(order.getUserId());
                             if (user != null) {
                                 res.setUsername(user.getUsername());
                             }
                             return res;
                         })
                         .toList();
    }

    @Override
    public List<BaseStatusRES<OrderStatus>> getStatus() {
        // 使用LambdaQueryWrapper进行分组统计
        List<Map<String, Object>> statusCounts = orderMapper.count();

        // 将统计结果转换为BaseStatusRES<OrderStatus>列表
        return statusCounts.stream()
            .map(map -> {
                BaseStatusRES<OrderStatus> baseStatusRES = new BaseStatusRES<OrderStatus>();
                baseStatusRES.setStatus((OrderStatus) map.get("state"));
                baseStatusRES.setCount((Integer) map.get("count"));
                baseStatusRES.setDescription(baseStatusRES.getStatus().getName());
                return baseStatusRES;
            })
            .collect(Collectors.toList());
    }
}