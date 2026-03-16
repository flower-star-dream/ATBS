package top.flowerstardream.atbs.order.api.v1.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.order.biz.service.ITicketService;
import top.flowerstardream.atbs.order.bo.dto.CancelTicketDTO;
import top.flowerstardream.atbs.order.bo.dto.TicketDTO;
import top.flowerstardream.atbs.order.bo.dto.TicketSeatReservationDTO;
import top.flowerstardream.base.result.Result;

import java.util.List;


/**
 * @Author: 花海
 * @Date: 2024/11/11
 * @Description: 内部机票服务接口（供其他服务调用）
 */
@RestController("internalTicketController")
@RequestMapping("/api/internal/v1/order/ticket")
@Tag(name = "内部服务-机票管理")
@Slf4j
public class TicketController {

    @Resource
    private ITicketService ticketService;

    /**
     * 获取机票
     * @param seatReservationId 座位预订ID
     * @return 机票
     */
    @GetMapping("/getTickets")
    Result<List<TicketSeatReservationDTO>> getTickets(@RequestParam Long seatReservationId){
        List<TicketSeatReservationDTO> tickets = ticketService.getTickets(seatReservationId);
        return Result.successResult(tickets);
    }
}