package top.flowerstardream.atbs.order.biz.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.order.bo.dto.*;
import top.flowerstardream.base.result.Result;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Author: 花海
 * @Date: 2025/11/11/16:06
 * @Description: 航空服务Feign客户端
 */
@FeignClient(name = "atbs-airplane", path = "/api/internal/v1/airplane")
public interface AirplaneClient {

    /**
     * 预订座位
     * @param reserveSeatDTO 预订座位参数
     * @return 座位预订号
     */
    @PostMapping("/seatReservation/reserve")
    Result<ReserveSeatResultDTO> reserveSeat(@RequestBody ReserveSeatDTO reserveSeatDTO);

    /**
     * 释放座位
     * @param seatReservationIds 座位预订号列表
     * @return 是否成功
     */
    @PostMapping("/seatReservation/release")
    Result<Void> releaseSeat(@RequestParam List<Long> seatReservationIds);

    /**
     * 查询余票数量
     * @param scheduleId 班次ID
     * @return 余票数量
     */
    @GetMapping("/schedule/remaining-count")
    Result<Integer> getRemainingTicketCount(@RequestParam("scheduleId") Integer scheduleId);

    /**
     * 计算机票价格
     * @param reserveSeatDTO
     * @return
     */
    @PostMapping("/routeStations/calc")
    Result<BigDecimal> calcTicketPrice(@RequestBody CalcTicketPriceDTO reserveSeatDTO);

    /**
     * 根据站名获取站ID
     * @param stationName
     * @return
     */
    @GetMapping("/station/by-name")
    Result<List<Long>> getStationIdsByName(@RequestParam("stationName") String stationName);

    /**
     * 根据座位预订ID获取座位预订号
     * @param seatReservationIds
     * @return
     */
    @PostMapping("/seatReservation/by-ids")
    Result<List<SeatReservationDTO>> getSeatReservationByIds(@RequestParam List<Long> seatReservationIds);

    /**
     * 根据站ID获取站名
     * @param stationIds
     * @return
     */
    @PostMapping("/station/by-ids")
    Result<List<StationsDTO>> getStationNamesByStationIds(@RequestParam List<Long> stationIds);

    /**
     * 根据班次ID获取站ID
     * @param scheduleId
     * @return
     */
    @GetMapping("/seatReservation/by-schedule-id")
    Result<List<Long>> getSeatReservationIdsByScheduleId(@RequestParam Long scheduleId);
}