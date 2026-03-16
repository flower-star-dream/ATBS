package top.flowerstardream.atbs.airplane.api.v1.mgmt;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.airplane.ao.pqreq.SeatReservationPageQueryREQ;
import top.flowerstardream.atbs.airplane.ao.req.SeatReservationChangeStatusREQ;
import top.flowerstardream.atbs.airplane.ao.req.SeatReservationREQ;
import top.flowerstardream.atbs.airplane.ao.res.SeatReservationRES;
import top.flowerstardream.atbs.airplane.biz.service.ISeatReservationService;
import top.flowerstardream.base.ao.res.BaseStatusRES;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.result.Result;
import top.flowerstardream.base.state.BaseStatus;

import java.util.List;

@RestController("mgmtSeatReservationController")
@RequestMapping("/api/mgmt/v1/airplane/seatReservation")
@Tag(name = "后管端-座位预订管理")
@Slf4j
public class MgmtSeatReservationController {

    @Resource
    private ISeatReservationService seatReservationService;

    @PostMapping("/addSeatReservation")
    public Result<Void> addSeatReservation(@RequestBody SeatReservationREQ seatReservationREQ) {
        log.info("【管理端-座位预订服务】添加座位预订，参数: {}", seatReservationREQ);
        seatReservationService.addSeatReservation(seatReservationREQ);
        return Result.successResult();
    }

    @PutMapping("/updateSeatReservation")
    public Result<Void> updateSeatReservation(@RequestBody SeatReservationREQ seatReservationREQ) {
        log.info("【管理端-座位预订服务】更新座位预订，参数: {}", seatReservationREQ);
        seatReservationService.updateSeatReservation(seatReservationREQ);
        return Result.successResult();
    }

    @DeleteMapping("/deleteSeatReservation")
    public Result<Void> deleteSeatReservation(@RequestBody List<Long> seatReservationIds) {
        log.info("【管理端-座位预订服务】删除座位预订，参数: {}", seatReservationIds);
        seatReservationService.deleteSeatReservation(seatReservationIds);
        return Result.successResult();
    }

    @GetMapping("/getSeatReservation")
    public Result<PageResult<SeatReservationRES>> seatReservationPageQuery(SeatReservationPageQueryREQ seatReservationPageQueryREQ) {
        log.info("【管理端-座位预订服务】获取座位预订，参数: {}", seatReservationPageQueryREQ);
        PageResult<SeatReservationRES> pageResult = seatReservationService.seatReservationPageQuery(seatReservationPageQueryREQ);
        return Result.successResult(pageResult);
    }

    @PutMapping("/batch-update-state")
    public Result<Void> batchUpdateStatus(@RequestBody SeatReservationChangeStatusREQ seatReservationChangeStatusREQ) {
        log.info("【管理端-座位预订服务】批量更新座位预订状态，参数: {}", seatReservationChangeStatusREQ);
        seatReservationService.batchUpdateStatus(seatReservationChangeStatusREQ);
        return Result.successResult();
    }

    @GetMapping("/getStatus")
    public Result<List<BaseStatusRES<BaseStatus>>> getStatus() {
        log.info("【管理端-座位预订服务】获取座位预订状态");
        List<BaseStatusRES<BaseStatus>> statusRES = seatReservationService.getStatus();
        return Result.successResult(statusRES);
    }
}
