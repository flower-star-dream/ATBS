package top.flowerstardream.atbs.order.api.v1.app;

import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.order.ao.req.OrderREQ;
import top.flowerstardream.atbs.order.ao.req.OrdersPaymentREQ;
import top.flowerstardream.atbs.order.ao.res.OrderMgmtRES;
import top.flowerstardream.atbs.order.ao.res.OrderPaymentRES;
import top.flowerstardream.atbs.order.ao.res.OrderRES;
import top.flowerstardream.atbs.order.biz.service.IOrderService;
import top.flowerstardream.base.result.Result;

import java.util.List;

import static top.flowerstardream.atbs.order.common.enums.OrderExceptionEnum.*;
import static top.flowerstardream.base.utils.GetInfoUtil.*;


/**
 * @Author: 花海
 * @Date: 2025/11/11
 * @Description: C端订单控制器
 */
@RestController("appOrderController")
@RequestMapping("/api/app/v1/order/order")
@Tag(name = "订单服务", description = "C端订单接口")
@Slf4j
public class OrderController {

    @Resource
    private IOrderService orderService;

    /**
     * 创建订单
     * @param req 订单创建请求
     * @return 订单ID
     */
    @Operation(summary = "创建订单", description = "C端创建新订单")
    @PostMapping
    public Result<String> createOrder(@RequestBody OrderREQ req) {
        orderService.createOrder(req);
        return Result.successResult();
    }

    /**
     * 查询订单详情
     * @param id 订单ID
     * @return 订单详情
     */
    @Operation(summary = "查询订单详情", description = "根据订单ID查询订单详情")
    @GetMapping("/{id}")
    public Result<OrderRES> getOrderById(@PathVariable Long id) {
        // 获取当前用户ID
        Long userId = getOperatorId();
        // 查询订单并验证归属
        OrderMgmtRES order = orderService.getOrderById(id);
        if (!order.getUserId().equals(userId)) {
            throw ORDER_PERMISSION_DENIED.toException();
        }
        OrderRES orderRes = new OrderRES();
        BeanUtils.copyProperties(order, orderRes);
        return Result.successResult(orderRes);
    }

    /**
     * 订单支付
     * @param ordersPaymentREQ
     * @return
     */
    @PutMapping("/payment")
    @Operation(summary = "订单支付", description = "C端订单支付")
    public Result<OrderPaymentRES> payment(@RequestBody OrdersPaymentREQ ordersPaymentREQ){
        log.info("订单支付：{}", ordersPaymentREQ);
        OrderPaymentRES orderPaymentVO = orderService.payment(ordersPaymentREQ);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.successResult(orderPaymentVO);
    }

    /**
     * 取消订单
     * @param orderId 订单ID
     */
    @Operation(summary = "取消订单", description = "C端取消订单")
    @DeleteMapping("/{orderId}")
    public Result<String> cancelOrder(@PathVariable Long orderId){
        // 获取当前用户ID
        Long userId = getOperatorId();
        orderService.cancelOrder(orderId, userId);
        return Result.successResult();
    }

    /**
     * 查询用户订单列表
     * @return 分页结果
     */
    @Operation(summary = "查询用户订单列表", description = "C端查询当前用户的订单列表")
    @GetMapping("/list")
    public Result<List<OrderRES>> getUserOrders() {
        // 获取当前用户ID
        Long userId = getOperatorId();
        return Result.successResult(orderService.getUserOrders(userId));
    }
}