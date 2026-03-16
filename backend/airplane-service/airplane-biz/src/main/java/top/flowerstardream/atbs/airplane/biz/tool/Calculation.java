package top.flowerstardream.atbs.airplane.biz.tool;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import top.flowerstardream.atbs.airplane.bo.dto.CalcTicketPriceDTO;
import top.flowerstardream.atbs.airplane.bo.dto.ReserveSeatDTO;
import top.flowerstardream.atbs.airplane.bo.dto.TimeDTO;
import top.flowerstardream.atbs.airplane.biz.mapper.RouteMapper;
import top.flowerstardream.atbs.airplane.biz.mapper.RouteStationsMapper;
import top.flowerstardream.atbs.airplane.biz.mapper.ScheduleMapper;
import top.flowerstardream.atbs.airplane.bo.eo.RouteEO;
import top.flowerstardream.atbs.airplane.bo.eo.RouteStationsEO;
import top.flowerstardream.atbs.airplane.bo.eo.ScheduleEO;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static top.flowerstardream.atbs.airplane.common.Common.PRICE_EACH_STATION;
import static top.flowerstardream.atbs.airplane.common.AirplaneExceptionEnum.*;


@Component
public class Calculation {

    @Resource
    private RouteMapper routeMapper;

    @Resource
    private RouteStationsMapper routeStationsMapper;

    @Resource
    private ScheduleMapper scheduleMapper;


    public TimeDTO timeCalculation(ReserveSeatDTO reserveSeatDTO){
        //根据班次ID查询飞机起始时间与终止时间
        LambdaQueryWrapper<ScheduleEO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ScheduleEO::getId, reserveSeatDTO.getScheduleId());
        ScheduleEO scheduleEO = scheduleMapper.selectOne(queryWrapper);
        //转换为时间戳
        LocalDateTime startTime = scheduleEO.getStartTime();
        long startMoment = startTime.atZone(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
        LocalDateTime endTime = scheduleEO.getEndTime();
        long endMoment = endTime.atZone(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
        //获取路线
        LambdaQueryWrapper<RouteEO> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(RouteEO::getId, scheduleEO.getRouteId());
        RouteEO routeEO = routeMapper.selectOne(queryWrapper1);
        //根据路线ID查询路线总站点数
        LambdaQueryWrapper<RouteStationsEO> queryWrapper2 = new LambdaQueryWrapper<>();
        queryWrapper2.eq(RouteStationsEO::getRouteId, routeEO.getId());
        Long totalStations = routeStationsMapper.selectCount(queryWrapper2);
        Integer totalStationsNum = totalStations.intValue();
        //获取起点站点数
        LambdaQueryWrapper<RouteStationsEO> queryWrapper3 = new LambdaQueryWrapper<>();
        queryWrapper3.eq(RouteStationsEO::getRouteId, routeEO.getId());
        queryWrapper3.eq(RouteStationsEO::getStationId, reserveSeatDTO.getStartStationId());
        RouteStationsEO routeStationsEO = routeStationsMapper.selectOne(queryWrapper3);
        Integer startStationSorting = routeStationsEO.getStationSorting();
        //获取终点站点数
        LambdaQueryWrapper<RouteStationsEO> queryWrapper4 = new LambdaQueryWrapper<>();
        queryWrapper4.eq(RouteStationsEO::getRouteId, routeEO.getId());
        queryWrapper4.eq(RouteStationsEO::getStationId, reserveSeatDTO.getEndStationId());
        RouteStationsEO routeStationsEO1 = routeStationsMapper.selectOne(queryWrapper4);
        Integer endStationSorting = routeStationsEO1.getStationSorting();
        //计算起始时间
        long startStationMoment = (startStationSorting/totalStationsNum) * (endMoment - startMoment)+startMoment;
        //计算终止时间
        long endStationMoment = (endStationSorting/totalStationsNum) * (endMoment - startMoment)+startMoment;
        //转回localDateTime
        LocalDateTime startStationTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startStationMoment), ZoneOffset.UTC);
        LocalDateTime endStationTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endStationMoment), ZoneOffset.UTC);

        return new TimeDTO(startStationTime, endStationTime);
     }

    public BigDecimal ticketPriceCalculation(CalcTicketPriceDTO calcTicketPriceDTO){
        //班次查路线
        LambdaQueryWrapper<ScheduleEO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ScheduleEO::getId, calcTicketPriceDTO.getScheduleId());
        ScheduleEO scheduleEO = scheduleMapper.selectOne(queryWrapper);
        //查出路线所有的站点
        LambdaQueryWrapper<RouteStationsEO> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.in(RouteStationsEO::getRouteId, scheduleEO.getRouteId());
        List<RouteStationsEO> routeStationsEOList = routeStationsMapper.selectList(queryWrapper1);
        //根据起点站ID和终点站ID获取对应的站点排序
        Integer startStationSorting = routeStationsEOList.stream()
                .filter(eo -> eo.getStationId().equals(calcTicketPriceDTO.getStartStationId()))
                .map(RouteStationsEO::getStationSorting)
                .findFirst()
                .orElse(null);
        Integer endStationSorting = routeStationsEOList.stream()
                .filter(eo -> eo.getStationId().equals(calcTicketPriceDTO.getEndStationId()))
                .map(RouteStationsEO::getStationSorting)
                .findFirst()
                .orElse(null);
        if (startStationSorting == null || endStationSorting == null) {
            throw THE_SORTING_INFORMATION_STATION_CANNOT_BE_FOUND.toException();
        }
        if (endStationSorting <= startStationSorting ){
            throw THE_TERMINAL_STATION_IS_LOCATED_BEFORE_THE_STARTING_STATION.toException();
        }
        BigDecimal stationCount = new BigDecimal(endStationSorting - startStationSorting);
        return stationCount.multiply(PRICE_EACH_STATION);
    }
}
