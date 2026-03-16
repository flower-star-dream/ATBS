package top.flowerstardream.atbs.airplane.api.v1.mgmt;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.airplane.ao.pqreq.AirplanePageQueryREQ;
import top.flowerstardream.atbs.airplane.ao.req.AirplaneREQ;
import top.flowerstardream.atbs.airplane.ao.res.AirplaneRES;
import top.flowerstardream.atbs.airplane.biz.service.IAirplaneService;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.result.Result;

import java.util.List;

@RestController("mgmtAirplaneController")
@RequestMapping("/api/mgmt/v1/airplane/airplane")
@Tag(name = "管理端-飞机接口服务")
@Slf4j
public class MgmtAirplaneController {

    @Resource
    private IAirplaneService airplaneService;

    @PostMapping("/addAirplane")
    public Result<Void> addAirplane(@RequestBody AirplaneREQ airplaneREQ) {
        log.info("【管理端-飞机接口】添加飞机，参数: {}", airplaneREQ);
        airplaneService.addAirplane(airplaneREQ);
        return Result.successResult();
    }

    @PutMapping("/updateAirplane")
    public Result<Void> updateAirplane(@RequestBody AirplaneREQ airplaneREQ) {
        log.info("【管理端-飞机接口】更新飞机，参数: {}", airplaneREQ);
        airplaneService.updateAirplane(airplaneREQ);
        return Result.successResult();
    }
    @DeleteMapping("/deleteAirplane")
    public Result<Void> deleteAirplane(@RequestBody List<Long> ids) {
        log.info("【管理端-飞机接口】删除飞机，参数: {}", ids);
        airplaneService.deleteAirplane(ids);
        return Result.successResult();
    }
    @GetMapping("/getAirplane")
    public Result<PageResult<AirplaneRES>> airplanePageQuery(AirplanePageQueryREQ airplanePageQueryREQ) {
        log.info("【管理端-飞机接口】查询飞机，参数: {}", airplanePageQueryREQ);
        PageResult<AirplaneRES> result = airplaneService.airplanePageQuery(airplanePageQueryREQ);
        return Result.successResult(result);
    }
}
