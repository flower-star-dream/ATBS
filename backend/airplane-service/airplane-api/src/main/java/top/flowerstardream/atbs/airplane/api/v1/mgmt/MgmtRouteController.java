package top.flowerstardream.atbs.airplane.api.v1.mgmt;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: QAQ
 * @Date: 2025/11/10 16:00
 * @Description: 用户接口
 */
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.airplane.ao.pqreq.RoutePageQueryREQ;
import top.flowerstardream.atbs.airplane.ao.req.RouteREQ;
import top.flowerstardream.atbs.airplane.ao.res.RouteRES;
import top.flowerstardream.atbs.airplane.biz.service.IRouteService;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.result.Result;

import java.util.List;


@RestController("mgmtRouteController")
@RequestMapping("/api/mgmt/v1/airplane/route")
@Tag(name = "后管端-路线管理")
@Slf4j
public class MgmtRouteController {

    @Resource
    private IRouteService routeService;

    @PostMapping("/addRoute")
    public Result<Void> addRoute(@RequestBody RouteREQ routeREQ) {
        log.info("【管理端-路线服务】添加路线，参数: {}", routeREQ);
        routeService.addRoute(routeREQ);
        return Result.successResult();
    }

    @DeleteMapping("/deleteRoute")
    public Result<Void> deleteRoute(@RequestBody List<Long> ids) {
        log.info("【管理端-路线服务】删除路线，参数: {}", ids);
        routeService.deleteRoute(ids);
        return Result.successResult();
    }

    @PutMapping("/updateRoute")
    public Result<Void> updateRoute(@RequestBody RouteREQ routeREQ) {
        log.info("【管理端-路线服务】修改路线，参数: {}", routeREQ);
        routeService.updateRoute(routeREQ);
        return Result.successResult();
    }

    @GetMapping("/getRoute")
    public Result<PageResult<RouteRES>> routePageQuery(RoutePageQueryREQ routePageQueryREQ) {
        log.info("【管理端-路线服务】查询路线，参数: {}", routePageQueryREQ);
        PageResult<RouteRES> result = routeService.routePageQuery(routePageQueryREQ);
        return Result.successResult(result);
    }


}
