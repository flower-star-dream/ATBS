package top.flowerstardream.atbs.order.biz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.flowerstardream.atbs.order.ao.req.OrderPageQueryREQ;
import top.flowerstardream.atbs.order.ao.req.OrderREQ;
import top.flowerstardream.atbs.order.ao.req.OrderStatusREQ;
import top.flowerstardream.atbs.order.ao.req.OrdersPaymentREQ;
import top.flowerstardream.atbs.order.ao.res.OrderMgmtRES;
import top.flowerstardream.atbs.order.ao.res.OrderPaymentRES;
import top.flowerstardream.atbs.order.ao.res.OrderRES;
import top.flowerstardream.atbs.order.bo.eo.OrderEO;
import top.flowerstardream.atbs.order.common.enums.OrderStatus;
import top.flowerstardream.base.ao.res.BaseStatusRES;
import top.flowerstardream.base.result.PageResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Author: 花海
 * @Date: 2025/11/11
 * @Description: 订单服务接口
 */
public interface IOrderService extends IService<OrderEO> {

    /**
     * 创建订单
     * @param req 订单创建请求
     * @return 订单ID
     */
    void createOrder(OrderREQ req);

    /**
     * 查询订单详情
     * @param id 订单ID
     * @return 订单详情
     */
    OrderMgmtRES getOrderById(Long id);

    /**
     * 更新订单状态
     * @param req 订单状态更新请求
     */
    void updateOrderStatus(OrderStatusREQ req);

    /**
     * 取消订单（退订）
     * @param orderId 订单ID
     * @param userId 用户ID
     */
    void cancelOrder(Long orderId, Long userId);

    /**
     * 分页查询订单列表
     * @param req 查询条件
     * @return 分页结果
     */
    PageResult<OrderMgmtRES> pageQuery(OrderPageQueryREQ req);

    /**
     * 查询用户的订单列表
     * @param userId 用户ID
     * @return 分页结果
     */
    List<OrderRES> getUserOrders(Long userId);

    /**
     * 更新订单总价
     * @param orderId 订单ID
     * @param newTotalPrice 新的总价
     */
    void updateTotalPrice(Long orderId, BigDecimal newTotalPrice);

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     * @param amount
     */
    void paySuccess(String outTradeNo, BigDecimal amount);

    /**
     * 订单支付
     * @param ordersPaymentREQ
     * @return
     */
    OrderPaymentRES payment(OrdersPaymentREQ ordersPaymentREQ);

    /**
     * 订单退款
     * @param orderId
     */
    void orderRefund(Long orderId);

    /**
     * 获取订单状态
     * @param orderId
     * @return
     */
    OrderStatus getOrderStatus(Long orderId);

    /**
     * 获取订单状态列表
     * @return
     */
    List<BaseStatusRES<OrderStatus>> getStatus();
}