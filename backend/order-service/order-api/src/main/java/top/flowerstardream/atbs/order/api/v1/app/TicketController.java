package top.flowerstardream.atbs.order.api.v1.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.order.ao.res.TicketRES;
import top.flowerstardream.atbs.order.biz.service.ITicketService;
import top.flowerstardream.base.result.Result;

import java.util.List;

import static top.flowerstardream.base.utils.GetInfoUtil.*;


/**
 * @Author: 花海
 * @Date: 2024/11/11
 * @Description: 小程序端机票控制器
 */
@RestController("appTicketController")
@RequestMapping("/api/app/v1/ticket/ticket")
@Tag(name = "小程序端-机票管理")
@Slf4j
public class TicketController {

    @Resource
    private ITicketService ticketService;

    /**
     * 获取当前用户的机票列表
     * @return 机票列表
     */
    @Operation(summary = "获取我的机票", description = "C端获取当前用户的机票列表")
    @GetMapping("/my-tickets")
    public Result<List<TicketRES>> getMyTickets() {
        Long userId = getOperatorId();
        log.info("【机票-小程序】traceId:{}, 获取用户机票列表，用户ID: {}", getTraceId(), userId);
        List<TicketRES> ticketList = ticketService.getTicketsByUserId(userId);
        return Result.successResult(ticketList);
    }

    /**
     * 取消机票
     * @param ticketId 机票ID
     * @return 操作结果
     */
    @Operation(summary = "取消机票", description = "C端取消指定机票")
    @PostMapping("/cancel/{ticketId}")
    public Result<Void> cancelTicket(@PathVariable("ticketId") Long ticketId) {
        Long userId = getOperatorId();
        log.info("【机票-小程序】traceId:{}, 取消机票，用户ID: {}, 机票ID: {}", 
                getTraceId(), userId, ticketId);
        ticketService.cancelTicket(ticketId, userId);
        return Result.successResult();
    }

    /**
     * 根据订单ID查询机票详情
     * @param orderId 订单ID
     * @return 机票列表
     */
    @Operation(summary = "根据订单查询机票", description = "C端根据订单ID查询机票列表")
    @GetMapping("/by-order/{orderId}")
    public Result<List<TicketRES>> getTicketsByOrderId(@PathVariable("orderId") Long orderId) {
        Long userId = getOperatorId();
        log.info("【机票-小程序】traceId:{}, 根据订单查询机票，用户ID: {}, 订单ID: {}", 
                getTraceId(), userId, orderId);

        // 这里需要转换DTO为RES
        List<TicketRES> ticketList = ticketService.getTicketsByOrderId(orderId);
        
        return Result.successResult(ticketList);
    }
}