package top.flowerstardream.atbs.order.api.v1.mgmt;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.order.ao.req.OrderPageQueryREQ;
import top.flowerstardream.atbs.order.ao.req.OrderStatusREQ;
import top.flowerstardream.atbs.order.ao.res.OrderMgmtRES;
import top.flowerstardream.atbs.order.biz.service.IOrderService;
import top.flowerstardream.atbs.order.common.enums.OrderStatus;
import top.flowerstardream.base.ao.res.BaseStatusRES;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.result.Result;

import java.util.List;

import static top.flowerstardream.base.utils.GetInfoUtil.*;


/**
 * @Author: 花海
 * @Date: 2025/11/11
 * @Description: 后管端订单控制器
 */
@RestController("mgmtOrderController")
@RequestMapping("/api/mgmt/v1/order/order")
@Tag(name = "订单管理", description = "B端订单管理接口")
@Slf4j
public class OrderController {

    @Resource
    private IOrderService orderService;

    /**
     * 分页查询订单列表
     * @param req 查询条件
     * @return 分页结果
     */
    @Operation(summary = "分页查询订单列表", description = "B端分页查询所有订单")
    @GetMapping("/page")
    public Result<PageResult<OrderMgmtRES>> pageQuery(OrderPageQueryREQ req) {
        log.info("【订单】traceId:{}, 分页查询订单列表：{}", getTraceId(), req);
        return Result.successResult(orderService.pageQuery(req));
    }

    /**
     * 查询订单详情
     * @param id 订单ID
     * @return 订单详情
     */
    @Operation(summary = "查询订单详情", description = "根据订单ID查询订单详情")
    @GetMapping("/{id}")
    public Result<OrderMgmtRES> getOrderById(@PathVariable Long id) {
        log.info("【订单】traceId:{}, 查询订单详情：{}", getTraceId(), id);
        return Result.successResult(orderService.getOrderById(id));
    }

    /**
     * 修改订单状态
     * @param req 订单状态修改请求
     */
    @Operation(summary = "修改订单状态", description = "B端修改订单状态")
    @PutMapping("/status")
    public Result<String> updateOrderStatus(@RequestBody OrderStatusREQ req) throws Exception {
        log.info("【订单】traceId:{}, 修改订单状态：{}", getTraceId(), req);
        orderService.updateOrderStatus(req);
        return Result.successResult();
    }

    /**
     * 获取订单状态列表
     * @return 订单状态列表
     */
    @Operation(summary = "获取订单状态列表", description = "获取订单状态列表")
    @GetMapping("/getStatus")
    public Result<List<BaseStatusRES<OrderStatus>>> getStatus() {
        log.info("【订单】traceId:{}, 获取订单状态列表", getTraceId());
        List<BaseStatusRES<OrderStatus>> statusRES = orderService.getStatus();
        return Result.successResult(statusRES);
    }
}