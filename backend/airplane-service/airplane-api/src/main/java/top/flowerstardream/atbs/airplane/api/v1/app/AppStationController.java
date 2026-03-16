package top.flowerstardream.atbs.airplane.api.v1.app;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.flowerstardream.atbs.airplane.ao.pqreq.StationPageQueryREQ;
import top.flowerstardream.atbs.airplane.ao.res.StationRES;
import top.flowerstardream.atbs.airplane.biz.service.IStationService;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.result.Result;

@RestController("appStationController")
@RequestMapping("/api/app/v1/airplane/station")
@Slf4j
public class AppStationController {

    @Resource
    private IStationService StationService;

    @GetMapping("/getStations")
    public Result<PageResult<StationRES>> userPageQuery(StationPageQueryREQ stationPageQueryREQ) {
        log.info("【小程序端-站点服务】查询站点，参数: {}", stationPageQueryREQ);
        PageResult<StationRES> result = StationService.UserPageQuery(stationPageQueryREQ);
        return Result.successResult(result);
    }
}
