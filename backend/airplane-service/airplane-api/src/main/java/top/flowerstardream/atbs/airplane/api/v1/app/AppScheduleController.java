package top.flowerstardream.atbs.airplane.api.v1.app;


import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.flowerstardream.atbs.airplane.ao.pqreq.RealTimeSchedulePageQueryREQ;
import top.flowerstardream.atbs.airplane.ao.req.RealTimeScheduleREQ;
import top.flowerstardream.atbs.airplane.ao.res.RealTimeScheduleRES;
import top.flowerstardream.atbs.airplane.biz.service.IScheduleService;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.result.Result;

@RestController("appScheduleController")
@RequestMapping("/api/app/v1/airplane/schedule")
@Tag(name = "小程序端-班次管理")
@Slf4j
public class AppScheduleController {

    @Resource
    private IScheduleService scheduleService;

    @GetMapping("/getSchedule")
    public Result<RealTimeScheduleRES> getSchedule(RealTimeScheduleREQ realTimeScheduleREQ) {
        log.info("【小程序端-班次服务】查询班次，参数: {}", realTimeScheduleREQ);
        RealTimeScheduleRES result = scheduleService.getSchedule(realTimeScheduleREQ);
        return Result.successResult(result);
    }

    @GetMapping("/realTimeSchedule")
    public Result<PageResult<RealTimeScheduleRES>> getRealTimeSchedule(RealTimeSchedulePageQueryREQ realTimeSchedulePageQueryREQ){
        log.info("【小程序端-班次服务】查询实时班次，参数: {}", realTimeSchedulePageQueryREQ);
        PageResult<RealTimeScheduleRES> realTimeScheduleRes = scheduleService.getRealTimeSchedule(realTimeSchedulePageQueryREQ);
        return Result.successResult(realTimeScheduleRes);
    };

}
