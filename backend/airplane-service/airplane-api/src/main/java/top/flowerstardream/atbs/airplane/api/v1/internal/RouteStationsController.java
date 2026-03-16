package top.flowerstardream.atbs.airplane.api.v1.internal;


import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.airplane.bo.dto.CalcTicketPriceDTO;
import top.flowerstardream.atbs.airplane.biz.service.IRouteStationsService;
import top.flowerstardream.base.result.Result;

import java.math.BigDecimal;

@RestController("internalRouteStationsController")
@RequestMapping("/api/internal/v1/airplane/routeStations")
@Tag(name = "路线站点接口服务")
@Slf4j
public class RouteStationsController {

    @Resource
    private IRouteStationsService routeStationsService;

    /**
     * 计算车票价格
     * @param calcTicketPriceDTO
     * @return
     */
    @PostMapping("/calc")
    public Result<BigDecimal> calcTicketPrice(@RequestBody CalcTicketPriceDTO calcTicketPriceDTO){
        log.info("【路线站点接口服务】计算车票价格，参数: {}", calcTicketPriceDTO);
        BigDecimal price = routeStationsService.calcTicketPrice(calcTicketPriceDTO);
        return Result.successResult(price);
    };
}
