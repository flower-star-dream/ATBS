package top.flowerstardream.atbs.airplane.api.v1.mgmt;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.airplane.ao.pqreq.RouteStationsPageQueryREQ;
import top.flowerstardream.atbs.airplane.ao.req.RouteStationsREQ;
import top.flowerstardream.atbs.airplane.ao.req.SortREQ;
import top.flowerstardream.atbs.airplane.ao.res.RouteStationsRES;
import top.flowerstardream.atbs.airplane.biz.service.IRouteStationsService;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.result.Result;

import java.util.List;

@RestController("mgmtRouteStationsController")
@RequestMapping("/api/mgmt/v1/airplane/routeStations")
@Tag(name = "后管端-路线站点管理")
@Slf4j
public class MgmtRouteStationsController {

    @Resource
    private IRouteStationsService routeStationsService;

    @PostMapping("/addRouteStations")
    public Result<Void> addRouteStations(@RequestBody RouteStationsREQ routeStationsREQ) {
        log.info("【后管端-路线站点服务】添加路线站点，参数: {}", routeStationsREQ);
        routeStationsService.addRouteStations(routeStationsREQ);
        return Result.successResult();
    }

    @PutMapping("/updateRouteStations")
    public Result<Void> updateRouteStations(@RequestBody RouteStationsREQ routeStationsREQ) {
        log.info("【后管端-路线站点服务】修改路线站点，参数: {}", routeStationsREQ);
        routeStationsService.updateRouteStations(routeStationsREQ);
        return Result.successResult();
    }

    @DeleteMapping("/deleteRouteStations")
    public Result<Void> deleteRouteStations(@RequestBody List<Long> ids) {
        log.info("【后管端-路线站点服务】删除路线站点，参数: {}", ids);
        routeStationsService.deleteRouteStations(ids);
        return Result.successResult();
    }

    @GetMapping("/getRouteStations")
    public Result<PageResult<RouteStationsRES>> routeStationsPageQuery(RouteStationsPageQueryREQ routeStationsPageQueryREQ){
        log.info("【后管端-路线站点服务】获取路线站点列表，参数: {}", routeStationsPageQueryREQ);
        PageResult<RouteStationsRES> routeStationsRESPageResult = routeStationsService.routeStationsPageQuery(routeStationsPageQueryREQ);
        return Result.successResult(routeStationsRESPageResult);
    }

    @PutMapping("/sort")
    public Result<Void> sort(@RequestBody SortREQ sortREQ) {
        log.info("【后管端-路线站点服务】排序路线站点，参数: {}", sortREQ);
        routeStationsService.sort(sortREQ);
        return Result.successResult();
    }
}
