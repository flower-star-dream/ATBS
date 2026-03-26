package top.flowerstardream.atbs.order.api.v1.mgmt;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.order.ao.req.TicketPageQueryREQ;
import top.flowerstardream.atbs.order.ao.req.TicketStatusChangeREQ;
import top.flowerstardream.atbs.order.ao.res.TicketRES;
import top.flowerstardream.atbs.order.biz.service.ITicketService;
import top.flowerstardream.atbs.order.common.enums.TicketStatus;
import top.flowerstardream.base.ao.res.BaseStatusRES;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.result.Result;

import java.util.List;

import static top.flowerstardream.base.utils.GetInfoUtil.*;


/**
 * @Author: 花海
 * @Date: 2024/11/11
 * @Description: 后管端机票控制器
 */
@RestController("mgmtTicketController")
@RequestMapping("/api/mgmt/v1/order/ticket")
@Tag(name = "后管端-机票管理")
@Slf4j
public class TicketController {

    @Resource
    private ITicketService ticketService;

    /**
     * 分页查询机票列表
     * @param req 查询条件
     * @return 分页结果
     */
    @Operation(summary = "分页查询机票", description = "B端分页查询机票列表")
    @GetMapping("/page")
    public Result<PageResult<TicketRES>> pageQuery(TicketPageQueryREQ req) {
        log.info("【机票-后管】traceId:{}, 分页查询机票，请求参数: {}", getTraceId(), req);
        PageResult<TicketRES> result = ticketService.pageQuery(req);
        return Result.successResult(result);
    }

    /**
     * 更新机票状态
     * @param req 状态变更请求
     * @return 操作结果
     */
    @Operation(summary = "更新机票状态", description = "B端更新机票状态")
    @PostMapping("/status")
    public Result<Void> updateStatus(@RequestBody TicketStatusChangeREQ req) {
        log.info("【机票-后管】traceId:{}, 更新机票状态，请求参数: {}", getTraceId(), req);
        ticketService.updateTicketStatus(req);
        return Result.successResult();
    }

    /**
     * 根据ID查询机票详情
     * @param id 机票ID
     * @return 机票详情
     */
    @Operation(summary = "查询机票详情", description = "B端根据ID查询机票详情")
    @GetMapping("/{id}")
    public Result<TicketRES> getByTicketId(@PathVariable("id") Long id) {
        log.info("【机票-后管】traceId:{}, 查询机票详情，机票ID: {}", getTraceId(), id);
        TicketRES ticketRES = ticketService.getByTicketId(id);
        return Result.successResult(ticketRES);
    }

    /**
     * 通过订单号查询机票详情
     * @param id 订单号
     * @return 机票详情
     */
    @Operation(summary = "通过订单号查询机票详情", description = "B端根据orderID查询机票详情")
    @GetMapping("/order/{orderId}")
    public Result<List<TicketRES>> getByOrderId(@PathVariable("orderId") Long id) {
        log.info("【机票-后管】traceId:{}, 查询机票详情，订单ID: {}", getTraceId(), id);
        List<TicketRES> ticketRES = ticketService.getByOrderId(id);
        return Result.successResult(ticketRES);
    }

    /**
     * 获取机票状态
     * @return 机票状态列表
     */
    @Operation(summary = "获取机票状态", description = "B端获取机票状态")
    @GetMapping("/getStatus")
    public Result<List<BaseStatusRES<TicketStatus>>> getStatus() {
        log.info("【机票-后管】traceId:{}, 获取机票状态", getTraceId());
        List<BaseStatusRES<TicketStatus>> statusRES = ticketService.getStatus();
        return Result.successResult(statusRES);
    }
}