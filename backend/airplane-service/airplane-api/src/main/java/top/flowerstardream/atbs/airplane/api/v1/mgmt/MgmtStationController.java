package top.flowerstardream.atbs.airplane.api.v1.mgmt;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.airplane.ao.pqreq.StationPageQueryREQ;
import top.flowerstardream.atbs.airplane.ao.req.StationREQ;
import top.flowerstardream.atbs.airplane.ao.res.StationMgmtRES;
import top.flowerstardream.atbs.airplane.biz.service.IStationService;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.result.Result;

import java.util.List;

@RestController("mgmtStationController")
@RequestMapping("/api/mgmt/v1/airplane/station")
@Tag(name = "管理端-站点管理")
@Slf4j
public class MgmtStationController {

    @Resource
    private IStationService stationService;

    @PostMapping("/addStation")
    public Result<Void> addStation(@RequestBody StationREQ stationREQ) {
        log.info("【管理端-添加站点】参数: {}", stationREQ);
        stationService.addStation(stationREQ);
        return Result.successResult();
    }

    @PutMapping("/updateStation")
    public Result<Void> updateStation(@RequestBody StationREQ stationREQ) {
        log.info("【管理端-修改站点】参数: {}", stationREQ);
        stationService.updateStation(stationREQ);
        return Result.successResult();
    }
    @DeleteMapping("/deleteStation")
    public Result<Void> deleteStation(@RequestBody List<Long> ids) {
        log.info("【管理端-删除站点】参数: {}", ids);
        stationService.deleteStation(ids);
        return Result.successResult();
    }
    @GetMapping("/getStation")
    public Result<PageResult<StationMgmtRES>> stationPageQuery(StationPageQueryREQ stationPageQueryREQ) {
        log.info("【管理端-查询站点】参数: {}", stationPageQueryREQ);
        PageResult<StationMgmtRES> result = stationService.stationPageQuery(stationPageQueryREQ);
        return Result.successResult(result);
    }

}
