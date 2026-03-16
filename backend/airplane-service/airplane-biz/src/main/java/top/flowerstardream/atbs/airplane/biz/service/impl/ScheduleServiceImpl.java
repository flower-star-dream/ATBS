package top.flowerstardream.atbs.airplane.biz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.flowerstardream.atbs.airplane.bo.dto.CalcTicketPriceDTO;
import top.flowerstardream.atbs.airplane.bo.dto.ReserveSeatDTO;
import top.flowerstardream.atbs.airplane.bo.dto.TimeDTO;
import top.flowerstardream.atbs.airplane.ao.pqreq.RealTimeSchedulePageQueryREQ;
import top.flowerstardream.atbs.airplane.ao.req.RealTimeScheduleREQ;
import top.flowerstardream.atbs.airplane.ao.req.ScheduleREQ;
import top.flowerstardream.atbs.airplane.ao.res.RealTimeScheduleRES;
import top.flowerstardream.atbs.airplane.ao.res.ScheduleRES;
import top.flowerstardream.atbs.airplane.biz.mapper.*;
import top.flowerstardream.atbs.airplane.biz.tool.Calculation;
import top.flowerstardream.atbs.airplane.ao.pqreq.SchedulePageQueryREQ;
import top.flowerstardream.atbs.airplane.biz.service.IScheduleService;
import top.flowerstardream.atbs.airplane.bo.eo.*;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.state.BaseStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static top.flowerstardream.atbs.airplane.common.AirplaneExceptionEnum.*;
import static top.flowerstardream.base.exception.BaseExceptionEnum.*;
import static top.flowerstardream.base.exception.ExceptionEnum.*;

@Slf4j
@Service
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper, ScheduleEO> implements IScheduleService {

    @Lazy
    @Resource
    private IScheduleService self;

    @Resource
    private ScheduleMapper scheduleMapper;

    @Resource
    private SeatReservationMapper seatReservationMapper;

    @Resource
    private RouteStationsMapper routeStationsMapper;

    @Resource
    private AirplaneMapper airplaneMapper;

    @Resource
    private Calculation calculation;

    @Resource
    private StationMapper stationMapper;

    @Resource
    private RouteMapper routeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSchedule(ScheduleREQ scheduleREQ) {
        //参数校验
        if (scheduleREQ == null) {
            throw THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }

        //判断班次存在，存在则中断
        validateScheduleIsExist(scheduleREQ.getAirplaneId(), scheduleREQ.getRouteId(), scheduleREQ.getStartTime());

        //获取飞机信息，包括座位总数
        AirplaneEO airplaneEO = airplaneMapper.selectById(scheduleREQ.getAirplaneId());
        if (airplaneEO == null) {
            throw THE_AIRPLANE_NOT_EXIST.toException();
        }

        //打包req的属性进入EO，然后插入数据库
        ScheduleEO scheduleEO = new ScheduleEO();
        BeanUtil.copyProperties(scheduleREQ, scheduleEO);
        scheduleEO.setAvailableTickets(airplaneEO.getSeatNum()); // 设置余票数量为飞机座位数
        boolean insert = self.save(scheduleEO);
        if (!insert) {
            throw INSERTION_FAILED.toException();
        }

        // 为该班次生成对应数量的座位预订记录，初始状态为未预订
        List<SeatReservationEO> seatReservations = new ArrayList<>();
        for (int i = 1; i <= airplaneEO.getSeatNum(); i++) {
            SeatReservationEO reservation = new SeatReservationEO();
            reservation.setScheduleId(scheduleEO.getId());
            reservation.setSeatNum(i);
            reservation.setStatus(BaseStatus.DISABLE);
            seatReservations.add(reservation);
        }
        
        // 批量插入座位预订记录
        if (CollUtil.isNotEmpty(seatReservations)) {
            seatReservationMapper.insertBatchSomeColumn(seatReservations);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSchedule(List<Long> ids) {
        if(CollUtil.isEmpty(ids)){
            return;
        }
        //排查有无使用路线
        ids.forEach(id -> {
            //获取班次信息
            ScheduleEO scheduleEO = self.getById(id);
            if (scheduleEO != null) {
                return;
            }

            LambdaQueryWrapper<SeatReservationEO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SeatReservationEO::getScheduleId,id);
            List<SeatReservationEO> seatReservations = seatReservationMapper.selectList(queryWrapper);

            if (CollUtil.isNotEmpty(seatReservations)){
                throw SCHEDULE_IS_USED.toException();
            }

        });
        //批量删除员工
        boolean delete = self.removeByIds(ids);
        if (!delete) {
            throw DELETION_FAILED.toException();
        }

    }

    @Override
    public void updateSchedule(ScheduleREQ scheduleREQ) {
        //参数校验
        if (scheduleREQ == null) {
            throw THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }
        ScheduleEO scheduleEO = new ScheduleEO();
        BeanUtil.copyProperties(scheduleREQ, scheduleEO);
        boolean update = self.updateById(scheduleEO);
        if (!update) {
            throw MODIFICATION_FAILED.toException();
        }

    }

    @Override
    public PageResult<ScheduleRES> schedulePageQuery(SchedulePageQueryREQ schedulePageQueryREQ) {
        //参数校验
        if (schedulePageQueryREQ == null) {
            throw THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }
        //设置分页参数默认值
        if (schedulePageQueryREQ.getPage() <= 0) {
            schedulePageQueryREQ.setPage(1);
        }
        if (schedulePageQueryREQ.getPageSize() <= 0) {
            schedulePageQueryREQ.setPageSize(10);
        }

        //创建分页对象
        Page<ScheduleEO> page = new Page<>(schedulePageQueryREQ.getPage(), schedulePageQueryREQ.getPageSize());
        //创建查询条件
        LambdaQueryWrapper<ScheduleEO> queryWrapper = new LambdaQueryWrapper<>();

        //查询条件
        if (schedulePageQueryREQ.getId() != null) {
            queryWrapper.eq(ScheduleEO::getId, schedulePageQueryREQ.getId());
        }
        if (schedulePageQueryREQ.getAirplaneId() != null) {
            queryWrapper.eq(ScheduleEO::getAirplaneId, schedulePageQueryREQ.getAirplaneId());
        }
        if (schedulePageQueryREQ.getRouteId() != null) {
            queryWrapper.eq(ScheduleEO::getRouteId, schedulePageQueryREQ.getRouteId());
        }
        if (StrUtil.isNotBlank(schedulePageQueryREQ.getCaptain())) {
            queryWrapper.like(ScheduleEO::getCaptain, schedulePageQueryREQ.getCaptain());
        }
        if (schedulePageQueryREQ.getStartTime() != null) {
            LocalDateTime startTime = schedulePageQueryREQ.getStartTime().withHour(0).withMinute(0).withSecond(0);
            LocalDateTime startTimeX = schedulePageQueryREQ.getStartTime().withHour(23).withMinute(59).withSecond(59);
            queryWrapper.between(ScheduleEO::getStartTime, startTime, startTimeX);
        }
        if (schedulePageQueryREQ.getEndTime() != null) {
            LocalDateTime endTime = schedulePageQueryREQ.getEndTime().withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endTimeX = schedulePageQueryREQ.getEndTime().withHour(23).withMinute(59).withSecond(59);
            queryWrapper.between(ScheduleEO::getEndTime, endTime, endTimeX);
        }

        //执行分页查询
        IPage<ScheduleEO> schedulePage = scheduleMapper.selectPage(page, queryWrapper);
        List<ScheduleEO> records = schedulePage.getRecords();

        List<Long> airplaneIds = records.stream().map(ScheduleEO::getAirplaneId).distinct().toList();
        List<Long> routeIds = records.stream().map(ScheduleEO::getRouteId).distinct().toList();
        List<AirplaneEO> airplanes = CollUtil.isEmpty(airplaneIds) ? Collections.emptyList() : airplaneMapper.selectBatchIds(airplaneIds);
        List<RouteEO> routes = CollUtil.isEmpty(routeIds) ? Collections.emptyList() : routeMapper.selectBatchIds(routeIds);
        Map<Long, String> airplaneNameMap = airplanes.stream()
                .collect(Collectors.toMap(AirplaneEO::getId, AirplaneEO::getAirplaneName));
        Map<Long, String> routeNameMap = routes.stream()
                .collect(Collectors.toMap(RouteEO::getId, RouteEO::getRouteName));
        //将EO转换为RES
        List<ScheduleRES> resList = schedulePage.getRecords().stream()
                .map(eo -> {
                    ScheduleRES res = new ScheduleRES();
                    BeanUtil.copyProperties(eo, res);
                    res.setAirplaneName(airplaneNameMap.get(eo.getAirplaneId()));
                    res.setRouteName(routeNameMap.get(eo.getRouteId()));
                    res.setAvailableTickets(eo.getAvailableTickets());
                    return res;
                })
                .toList();

        //封装返回结果
        PageResult<ScheduleRES> pageResult = new PageResult<>();
        pageResult.setTotal(schedulePage.getTotal());
        pageResult.setRecords(resList);
        return pageResult;
    }

    @Override
    public RealTimeScheduleRES getSchedule(RealTimeScheduleREQ realTimeScheduleREQ) {
        if (realTimeScheduleREQ == null) {
            throw THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }
        ScheduleEO scheduleEO = self.getById(realTimeScheduleREQ.getScheduleId());

        /**
         * 站点ID查站名
         */
        String startStation = stationMapper.selectById(realTimeScheduleREQ.getStartStationId()).getStationName();
        String endStation = stationMapper.selectById(realTimeScheduleREQ.getEndStationId()).getStationName();

        /**
         * 获取出发时间和到达时间
         */
        ReserveSeatDTO reserveSeatDTO = ReserveSeatDTO.builder()
                .scheduleId(realTimeScheduleREQ.getScheduleId())
                .startStationId(realTimeScheduleREQ.getStartStationId())
                .endStationId(realTimeScheduleREQ.getEndStationId())
                .build();
        TimeDTO timeDTO = calculation.timeCalculation(reserveSeatDTO);

        /**
         * 计算票价
         */
        CalcTicketPriceDTO calcTicketPriceDTO = CalcTicketPriceDTO.builder()
                .scheduleId(realTimeScheduleREQ.getScheduleId())
                .startStationId(realTimeScheduleREQ.getStartStationId())
                .endStationId(realTimeScheduleREQ.getEndStationId())
                .build();
        BigDecimal price = calculation.ticketPriceCalculation(calcTicketPriceDTO);


        return RealTimeScheduleRES.builder()
                .scheduleId(scheduleEO.getId())
                .startTime(timeDTO.getStartStationTime())
                .endTime(timeDTO.getEndStationTime())
                .startStation(startStation)
                .endStation(endStation)
                .price(price)
                .remainTicket(scheduleEO.getAvailableTickets())
                .build();
    }

    private ScheduleEO getSchedule(Long airplaneId, Long routeId, LocalDateTime startTime){
        LambdaQueryWrapper<ScheduleEO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ScheduleEO::getAirplaneId, airplaneId)
                .eq(ScheduleEO::getRouteId, routeId)
                .eq(ScheduleEO::getStartTime, startTime);
        return scheduleMapper.selectOne(queryWrapper);
    }

    private void validateScheduleIsExist(Long airplaneId, Long routeId, LocalDateTime startTime) {
        ScheduleEO scheduleEO = getSchedule(airplaneId,routeId,startTime);
        if (scheduleEO != null) {
            throw SCHEDULE_ALREADY_EXISTS.toException();
        }
    }



    /**外部调用*/
    @Override
    public Integer getAvailingTickets(Long scheduleId){
        ScheduleEO scheduleEO = scheduleMapper.selectById(scheduleId);
        return scheduleEO.getAvailableTickets();
    }

    @Override
    public PageResult<RealTimeScheduleRES> getRealTimeSchedule(RealTimeSchedulePageQueryREQ realTimeSchedulePageQueryREQ){
        //参数校验
        if (realTimeSchedulePageQueryREQ == null) {
            throw THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }
        if (realTimeSchedulePageQueryREQ.getNowTime() == null) {
            throw PARAM_ERROR.toException();
        }

        //设置分页参数默认值
        if (realTimeSchedulePageQueryREQ.getPage() <= 0) {
            realTimeSchedulePageQueryREQ.setPage(1);
        }
        if (realTimeSchedulePageQueryREQ.getPageSize() <= 0) {
            realTimeSchedulePageQueryREQ.setPageSize(10);
        }

        /**
         * 站点ID查站名
         * */
        String startStation = stationMapper.selectById(realTimeSchedulePageQueryREQ.getStartStationId()).getStationName();
        String endStation = stationMapper.selectById(realTimeSchedulePageQueryREQ.getEndStationId()).getStationName();


        /**查找班次*/
        LambdaQueryWrapper<RouteStationsEO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteStationsEO::getStationId, realTimeSchedulePageQueryREQ.getStartStationId());
        List<RouteStationsEO> routeStationsStart = routeStationsMapper.selectList(queryWrapper);

        LambdaQueryWrapper<RouteStationsEO> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(RouteStationsEO::getStationId, realTimeSchedulePageQueryREQ.getEndStationId());
        List<RouteStationsEO> routeStationsEnd = routeStationsMapper.selectList(queryWrapper1);

        //找出同时包含出发站和终点站的路线ID
        List<Long> startRouteIds = routeStationsStart.stream()
                .map(RouteStationsEO::getRouteId)
                .toList();

        List<Long> commonRouteIds = routeStationsEnd.stream()
                .map(RouteStationsEO::getRouteId)
                .filter(startRouteIds::contains)
                .toList();

        //处理出发时间
        LocalDateTime startTime = realTimeSchedulePageQueryREQ.getNowTime().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endTime = realTimeSchedulePageQueryREQ.getNowTime().withHour(23).withMinute(59).withSecond(59);

        //根据路线和出发时间查询班次
        Page<ScheduleEO> page = new Page<>(realTimeSchedulePageQueryREQ.getPage(), realTimeSchedulePageQueryREQ.getPageSize());
        LambdaQueryWrapper<ScheduleEO> schedulequeryWrapper = new LambdaQueryWrapper<>();
        schedulequeryWrapper.between(ScheduleEO::getStartTime, startTime, endTime)
                .in(ScheduleEO::getRouteId, commonRouteIds);

        IPage<ScheduleEO> schedules = scheduleMapper.selectPage(page, schedulequeryWrapper);
        List<ScheduleEO> records = schedules.getRecords();

        //每个班次都封装出一个实时班次响应
        List<RealTimeScheduleRES> resList = records.stream()
                .map(schedule -> {
                    RealTimeScheduleRES res = new RealTimeScheduleRES();
                    //计算时间
                    ReserveSeatDTO reserveSeatDTO = ReserveSeatDTO.builder()
                            .scheduleId(schedule.getId())
                            .startStationId(realTimeSchedulePageQueryREQ.getStartStationId())
                            .endStationId(realTimeSchedulePageQueryREQ.getEndStationId())
                            .build();
                    TimeDTO timeDTO = calculation.timeCalculation(reserveSeatDTO);
                    //计算价格
                    BigDecimal price = calculation.ticketPriceCalculation(new CalcTicketPriceDTO(
                            schedule.getId(),
                            realTimeSchedulePageQueryREQ.getStartStationId(),
                            realTimeSchedulePageQueryREQ.getEndStationId()));
                    //封装结果
                    res.setScheduleId(schedule.getId());
                    res.setPrice(price);
                    res.setStartTime(timeDTO.getStartStationTime());
                    res.setEndTime(timeDTO.getEndStationTime());
                    res.setStartStation(startStation);
                    res.setEndStation(endStation);
                    res.setRemainTicket(schedule.getAvailableTickets());
                    return res;
                }).toList();

        PageResult<RealTimeScheduleRES> pageResult = new PageResult<>();
        pageResult.setTotal(schedules.getTotal());
        pageResult.setRecords(resList);
        return pageResult;
    }


}
