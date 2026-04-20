package top.flowerstardream.atbs.airplane.biz.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestParam;
import top.flowerstardream.atbs.airplane.bo.dto.TicketSeatReservationDTO;
import top.flowerstardream.base.result.Result;

import java.util.List;

/**
 * @author: QAQ
 * @date: 2025/11/25 16:28
 * @description: 订单服务客户端接口
 */
@FeignClient(name = "order-service", path = "/api/internal/v1/order")
public interface OrderClient {
    /**
     * 获取车票对象
     *
     */

    @GetMapping( "/ticket/getTickets")
    Result<List<TicketSeatReservationDTO>> getTickets(@RequestParam Long seatReservationId);

}
