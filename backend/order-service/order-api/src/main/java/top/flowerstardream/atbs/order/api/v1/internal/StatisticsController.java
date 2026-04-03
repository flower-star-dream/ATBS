package top.flowerstardream.atbs.order.api.v1.internal;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.flowerstardream.atbs.order.biz.service.ITicketService;
import top.flowerstardream.base.result.Result;

import java.math.BigDecimal;

/**
 * @Author: 花海
 * @Date: 2026/04/01/23:29
 * @Description: 统计服务接口（供其他服务调用）
 */
@RestController("internalStatisticsController")
@RequestMapping("/api/internal/v1/order/statistics")
@Tag(name = "内部服务-统计服务")
@Slf4j
public class StatisticsController {
    @Resource
    private ITicketService ticketService;

    @GetMapping("/daily-passengers")
    public Result<BigDecimal> getDailyPassengers(@RequestParam String date){
        log.info("【统计服务】获取某日乘客数");
        return Result.successResult(ticketService.getDailyPassengers(date));
    }

}
