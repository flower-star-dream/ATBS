package top.flowerstardream.atbs.airplane.api.v1.mgmt;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.airplane.ao.pqreq.RealTimeSchedulePageQueryREQ;
import top.flowerstardream.atbs.airplane.ao.pqreq.SchedulePageQueryREQ;
import top.flowerstardream.atbs.airplane.ao.req.ScheduleREQ;
import top.flowerstardream.atbs.airplane.ao.res.RealTimeScheduleRES;
import top.flowerstardream.atbs.airplane.ao.res.ScheduleRES;
import top.flowerstardream.atbs.airplane.biz.service.IScheduleService;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.result.Result;

import java.util.List;

@RestController("MgmtScheduleController")
@RequestMapping("/api/mgmt/v1/airplane/schedule")
@Tag(name = "后管端-班次管理")
@Slf4j
public class MgmtScheduleController {

    @Resource
    private IScheduleService scheduleService;

    @PostMapping("/addSchedule")
    public Result<Void> addSchedule(@RequestBody ScheduleREQ scheduleREQ) {
        log.info("【后管端-班次服务】添加班次，参数: {}", scheduleREQ);
        scheduleService.addSchedule(scheduleREQ);
        return Result.successResult();
    }
    @PutMapping("/updateSchedule")
    public Result<Void> updateSchedule(@RequestBody ScheduleREQ scheduleREQ) {
        log.info("【后管端-班次服务】修改班次，参数: {}", scheduleREQ);
        scheduleService.updateSchedule(scheduleREQ);
        return Result.successResult();
    }
    @DeleteMapping("/deleteSchedule")
    public Result<Void> deleteSchedule(@RequestBody List<Long> ids) {
        log.info("【后管端-班次服务】删除班次，参数: {}", ids);
        scheduleService.deleteSchedule(ids);
        return Result.successResult();
    }
    @GetMapping("/getSchedules")
    public Result<PageResult<ScheduleRES>> schedulePageQuery(SchedulePageQueryREQ schedulePageQueryREQ) {
        log.info("【后管端-班次服务】查询班次，参数: {}", schedulePageQueryREQ);
        PageResult<ScheduleRES> pageResult = scheduleService.schedulePageQuery(schedulePageQueryREQ);
        return Result.successResult(pageResult);
    }
    @GetMapping("/realTimeSchedule")
    public Result<PageResult<RealTimeScheduleRES>> getRealTimeSchedule(RealTimeSchedulePageQueryREQ realTimeSchedulePageQueryREQ){
        log.info("【后管端-班次服务】查询实时班次，参数: {}", realTimeSchedulePageQueryREQ);
        PageResult<RealTimeScheduleRES> realTimeScheduleRes = scheduleService.getRealTimeSchedule(realTimeSchedulePageQueryREQ);
        return Result.successResult(realTimeScheduleRes);
    };
}
