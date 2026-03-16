package top.flowerstardream.atbs.airplane.api.v1.internal;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.airplane.bo.dto.ReserveSeatDTO;
import top.flowerstardream.atbs.airplane.bo.dto.ReserveSeatResultDTO;
import top.flowerstardream.atbs.airplane.bo.dto.SeatReservationDTO;
import top.flowerstardream.atbs.airplane.biz.service.ISeatReservationService;
import top.flowerstardream.base.result.Result;

import java.util.List;

@RestController("internalSeatReservationController")
@RequestMapping("/api/internal/v1/airplane/seatReservation")
@Tag(name = "座位预订接口服务")
@Slf4j
public class SeatReservationController {

    @Resource
    private ISeatReservationService seatReservationService;

    @Operation(summary = "根据座位预订ID获取座位预订信息", description = "班次接口服务，根据座位预订ID获取座位预订信息")
    @PostMapping("/by-ids")
    public Result<List<SeatReservationDTO>> getSeatReservationByIds(@RequestParam List<Long> seatReservationIds){
        log.info("【班次接口服务】根据座位预订ID获取座位预订信息，座位预订ID: {}", seatReservationIds);
        List<SeatReservationDTO> seatReservationDTOs = seatReservationService.getSeatReservationByIds(seatReservationIds);
        return Result.successResult(seatReservationDTOs);
    };

    /**
     * 释放座位
     * @param seatReservationIds 座位预订号列表
     * @return 是否成功
     */
    @PostMapping("/release")
    public Result<Void> releaseSeat(@RequestParam List<Long> seatReservationIds){
        log.info("【班次接口服务】释放座位，座位预订号列表: {}", seatReservationIds);
        seatReservationService.releaseSeat(seatReservationIds);
        return Result.successResult();
    };

    /**
     * 预订座位
     * @param reserveSeatDTO 预订座位参数
     * @return 座位预订号
     */
    @PostMapping("/reserve")
    public Result<ReserveSeatResultDTO> reserveSeat(@RequestBody ReserveSeatDTO reserveSeatDTO){
        log.info("【班次接口服务】预订座位，参数: {}", reserveSeatDTO);
        ReserveSeatResultDTO reserveSeatResultDTO = seatReservationService.reserveSeat(reserveSeatDTO);
        return Result.successResult(reserveSeatResultDTO);
    };

    @GetMapping("/by-schedule-id")
    public Result<List<Long>> getSeatReservationIdsByScheduleId(@RequestParam Long scheduleId){
        log.info("【班次接口服务】根据班次ID获取座位预订号列表，班次ID: {}", scheduleId);
        List<Long> seatReservationIds = seatReservationService.getSeatReservationIdsByScheduleId(scheduleId);
        return Result.successResult(seatReservationIds);
    }
}
