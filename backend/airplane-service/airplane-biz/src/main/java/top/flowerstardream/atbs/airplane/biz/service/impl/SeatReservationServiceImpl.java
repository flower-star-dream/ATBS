package top.flowerstardream.atbs.airplane.biz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import top.flowerstardream.atbs.airplane.ao.req.SeatReservationChangeStatusREQ;
import top.flowerstardream.atbs.airplane.ao.req.SeatReservationREQ;
import top.flowerstardream.atbs.airplane.ao.res.SeatReservationRES;
import top.flowerstardream.atbs.airplane.biz.mapper.RouteMapper;
import top.flowerstardream.atbs.airplane.biz.mapper.ScheduleMapper;
import top.flowerstardream.atbs.airplane.biz.mapper.AirplaneMapper;
import top.flowerstardream.atbs.airplane.bo.eo.AirplaneEO;
import top.flowerstardream.atbs.airplane.bo.eo.RouteEO;
import top.flowerstardream.atbs.airplane.bo.eo.ScheduleEO;
import top.flowerstardream.atbs.airplane.bo.eo.SeatReservationEO;
import top.flowerstardream.atbs.airplane.ao.pqreq.SeatReservationPageQueryREQ;
import top.flowerstardream.atbs.airplane.biz.client.OrderClient;
import top.flowerstardream.atbs.airplane.biz.mapper.SeatReservationMapper;
import top.flowerstardream.atbs.airplane.biz.service.ISeatReservationService;
import top.flowerstardream.atbs.airplane.biz.tool.Calculation;
import top.flowerstardream.atbs.airplane.bo.dto.*;
import top.flowerstardream.base.ao.res.BaseStatusRES;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.result.Result;
import top.flowerstardream.base.service.Impl.BaseServiceImpl;
import top.flowerstardream.base.state.BaseStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static top.flowerstardream.atbs.airplane.common.AirplaneExceptionEnum.*;
import static top.flowerstardream.base.exception.BaseExceptionEnum.*;
import static top.flowerstardream.base.exception.ExceptionEnum.*;
import static top.flowerstardream.base.state.BaseStatus.DISABLE;
import static top.flowerstardream.base.state.BaseStatus.ENABLE;

@Slf4j
@Service
public class SeatReservationServiceImpl extends BaseServiceImpl<SeatReservationMapper, SeatReservationEO> implements ISeatReservationService {

    @Resource
    private SeatReservationMapper seatReservationMapper;

    @Resource
    private ScheduleMapper scheduleMapper;

    @Resource
    private AirplaneMapper airplaneMapper;

    @Resource
    private RouteMapper routeMapper;

    @Resource
    private Calculation calculation;

    @Lazy
    @Resource
    private ISeatReservationService self;

    @Resource
    private OrderClient orderClient;


    @Override
    public void addSeatReservation(SeatReservationREQ seatReservationREQ) {
        //参数校验
        if (seatReservationREQ == null) {
            throw THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }

        //判断座位预订存在，存在则中断
        validateSeatReservationIsExist(seatReservationREQ.getScheduleId(), seatReservationREQ.getSeatNum(), seatReservationREQ.getBookingStatus());

        SeatReservationEO seatReservationEO = new SeatReservationEO();
        BeanUtil.copyProperties(seatReservationREQ, seatReservationEO);
        boolean insert = self.save(seatReservationEO);
        if (!insert) {
            throw INSERTION_FAILED.toException();
        }
    }

    @Override
    public void deleteSeatReservation(List<Long> ids) {
        if(CollUtil.isEmpty(ids)){
            return;
        }

        ids.forEach(id -> {
            //获取路线信息
            SeatReservationEO seatReservationEO = self.getById(id);
            if (seatReservationEO == null) {
                return;
            }
            Result<List<TicketSeatReservationDTO>> tickets = orderClient.getTickets(id);

            if (CollUtil.isNotEmpty(tickets.getData())) {
                throw SEAT_RESERVATION_IS_USED.toException();
            }
        });


        //批量删除座位预订
        boolean delete = self.removeByIds(ids);
        if (!delete) {
            throw DELETION_FAILED.toException();
        }

    }

    @Override
    public void updateSeatReservation(SeatReservationREQ seatReservationREQ) {
        //参数校验
        if (seatReservationREQ == null) {
            throw THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }
        SeatReservationEO seatReservationEO = new SeatReservationEO();
        BeanUtil.copyProperties(seatReservationREQ, seatReservationEO);
        boolean update = self.updateById(seatReservationEO);
        if (!update) {
            throw MODIFICATION_FAILED.toException();
        }

    }

    @Override
    public PageResult<SeatReservationRES> seatReservationPageQuery(SeatReservationPageQueryREQ seatReservationPageQueryREQ) {
        //参数校验
        if (seatReservationPageQueryREQ == null) {
            throw THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }
        //设置分页参数默认值
        if (seatReservationPageQueryREQ.getPage() <= 0) {
            seatReservationPageQueryREQ.setPage(1);
        }
        if (seatReservationPageQueryREQ.getPageSize() <= 0) {
            seatReservationPageQueryREQ.setPageSize(10);
        }

        //创建分页对象
        Page<SeatReservationEO> page = new Page<>(seatReservationPageQueryREQ.getPage(), seatReservationPageQueryREQ.getPageSize());
        //创建查询条件
        LambdaQueryWrapper<SeatReservationEO> queryWrapper = Wrappers.lambdaQuery();

        //查询条件
        if (seatReservationPageQueryREQ.getId() != null) {
            queryWrapper.eq(SeatReservationEO::getId, seatReservationPageQueryREQ.getId());
        }
        if (seatReservationPageQueryREQ.getScheduleId() != null) {
            queryWrapper.eq(SeatReservationEO::getScheduleId, seatReservationPageQueryREQ.getScheduleId());
        }
        if (seatReservationPageQueryREQ.getSeatNum() != null) {
            queryWrapper.eq(SeatReservationEO::getSeatNum, seatReservationPageQueryREQ.getSeatNum());
        }
        if (seatReservationPageQueryREQ.getStatus() != null) {
            queryWrapper.eq(SeatReservationEO::getStatus, seatReservationPageQueryREQ.getStatus());
        }

        //执行分页查询
        Page<SeatReservationEO> seatReservationResult = seatReservationMapper.selectPage(page, queryWrapper);

        List<SeatReservationEO> records = seatReservationResult.getRecords();
        List<SeatReservationRES> resList = new ArrayList<>();
        
        if (CollUtil.isNotEmpty(records)) {
            // 提取班次ID列表
            List<Long> scheduleIds = records.stream()
                    .map(SeatReservationEO::getScheduleId)
                    .distinct()
                    .toList();
            
            // 批量查询班次信息
            List<ScheduleEO> schedules = scheduleMapper.selectBatchIds(scheduleIds);
            Map<Long, ScheduleEO> scheduleMap = schedules.stream()
                    .collect(Collectors.toMap(ScheduleEO::getId, s -> s));
            
            // 提取飞机和路线ID列表
            List<Long> airplaneIds = schedules.stream()
                    .map(ScheduleEO::getAirplaneId)
                    .distinct()
                    .toList();
            List<Long> routeIds = schedules.stream()
                    .map(ScheduleEO::getRouteId)
                    .distinct()
                    .toList();
            
            // 批量查询飞机和路线信息
            List<AirplaneEO> airplanes = CollUtil.isEmpty(airplaneIds) ? Collections.emptyList() : airplaneMapper.selectBatchIds(airplaneIds);
            List<RouteEO> routes = CollUtil.isEmpty(routeIds) ? Collections.emptyList() : routeMapper.selectBatchIds(routeIds);
            
            // 构建映射关系
            Map<Long, String> airplaneNameMap = airplanes.stream()
                    .collect(Collectors.toMap(AirplaneEO::getId, AirplaneEO::getAirplaneName));
            Map<Long, String> routeNameMap = routes.stream()
                    .collect(Collectors.toMap(RouteEO::getId, RouteEO::getRouteName));
            
            // 转换为RES对象
            resList = records.stream()
                    .map(eo -> {
                        SeatReservationRES res = new SeatReservationRES();
                        BeanUtil.copyProperties(eo, res);
                        
                        ScheduleEO schedule = scheduleMap.get(eo.getScheduleId());
                        if (schedule != null) {
                            res.setAirplaneName(airplaneNameMap.get(schedule.getAirplaneId()));
                            res.setRouteName(routeNameMap.get(schedule.getRouteId()));
                        }
                        
                        return res;
                    })
                    .toList();
        }

        //封装返回结果
        PageResult<SeatReservationRES> pageResult = new PageResult<>();
        pageResult.setTotal(seatReservationResult.getTotal());
        pageResult.setRecords(resList);
        return pageResult;
    }

    private SeatReservationEO getSeatReservationEO(Long scheduleId, Integer seatNum, Integer bookingStatus){
        LambdaQueryWrapper<SeatReservationEO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(SeatReservationEO::getScheduleId, scheduleId)
                .eq(SeatReservationEO::getSeatNum, seatNum)
                .eq(SeatReservationEO::getStatus, bookingStatus) ;
        return seatReservationMapper.selectOne(queryWrapper);
    }
    private void validateSeatReservationIsExist(Long scheduleId, Integer seatNum, Integer bookingStatus){
        SeatReservationEO seatReservationEO = getSeatReservationEO(scheduleId, seatNum, bookingStatus);
        if (seatReservationEO != null) {
            throw SEAT_RESERVATION_ALREADY_EXISTS.toException();
        }
    }


    /**
     * 外部调用
     * */
    @Override
    public List<SeatReservationDTO> getSeatReservationByIds(List<Long> seatReservationIds){
        //参数校验
        if (seatReservationIds == null || seatReservationIds.isEmpty()) {
            throw THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }

        LambdaQueryWrapper<SeatReservationEO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(SeatReservationEO::getId, seatReservationIds);
        List<SeatReservationEO> seatReservationEOs = seatReservationMapper.selectList(queryWrapper);

        return seatReservationEOs.stream().map(seatReservationEO -> SeatReservationDTO.builder()
                .id(seatReservationEO.getId())
                .scheduleId(seatReservationEO.getScheduleId())
                .seatNum(seatReservationEO.getSeatNum())
                .build()).toList();
    }
    /**
     * 外部调用
     * */
    @Override
    public void releaseSeat(@RequestParam("seatReservationIds") List<Long> seatReservationIds){

        //参数校验
        if (seatReservationIds == null || seatReservationIds.isEmpty()) {
            throw THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }
        List<SeatReservationEO> seatReservationEOs = seatReservationMapper.selectBatchIds(seatReservationIds);
        // 按照 scheduleId 分组统计每个班次需要增加的余票数
        Map<Long, Long> scheduleTicketCountMap = seatReservationEOs.stream()
                .collect(Collectors.groupingBy(SeatReservationEO::getScheduleId, Collectors.counting()));
        // 对每个 scheduleId 更新对应的余票数
        scheduleTicketCountMap.forEach((scheduleId, ticketCount) -> {
            LambdaUpdateWrapper<ScheduleEO> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.setSql("available_tickets = available_tickets + {0}", ticketCount)
                         .eq(ScheduleEO::getId, scheduleId);
            scheduleMapper.update(updateWrapper);
        });
            //修改座位预订状态
        for (SeatReservationEO seatReservationEO : seatReservationEOs) {
            seatReservationEO.setStatus(DISABLE);
        }
        self.updateBatchById(seatReservationEOs);
    }
    /**
     * 外部调用
     * */
    @Override
    public ReserveSeatResultDTO reserveSeat(ReserveSeatDTO reserveSeatDTO){
        //参数校验
        if (reserveSeatDTO == null) {
            throw THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }

        //根据用户的所选班次、起点站和终点站ID，计算上车时间和下车时间
        TimeDTO timeDTO = calculation.timeCalculation(reserveSeatDTO);
        LocalDateTime startStationTime = timeDTO.getStartStationTime();
        LocalDateTime endStationTime = timeDTO.getEndStationTime();
        /**
         * seatNumList
         */
        // 查询并扣除余票
        ScheduleEO scheduleEO = scheduleMapper.selectById(reserveSeatDTO.getScheduleId());
        Integer availingTickets = scheduleEO.getAvailableTickets();
        if (availingTickets < reserveSeatDTO.getTicketCount()) {
            throw NOT_ENOUGH_TICKETS.toException();
        }
        LambdaUpdateWrapper<ScheduleEO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.setSql("available_tickets = available_tickets - {0}", reserveSeatDTO.getTicketCount())
                    .eq(ScheduleEO::getId, reserveSeatDTO.getScheduleId())
                    .gt(ScheduleEO::getAvailableTickets, 0);
        int update = scheduleMapper.update(updateWrapper);
        if (update <= 0) {
            throw NOT_ENOUGH_TICKETS.toException();
        }
        //根据班次号查询座位信息
        LambdaQueryWrapper<SeatReservationEO> queryWrapper0 = new LambdaQueryWrapper<>();
        queryWrapper0.eq(SeatReservationEO::getScheduleId, reserveSeatDTO.getScheduleId())
                .eq(SeatReservationEO::getStatus, DISABLE);
        List<SeatReservationEO> seatReservationEOs = seatReservationMapper.selectList(queryWrapper0);
        if (CollUtil.isEmpty(seatReservationEOs)) {
            throw SEAT_RESERVATION_IS_USED.toException();
        }
        if (seatReservationEOs.size() < reserveSeatDTO.getTicketCount()) {
            throw NOT_ENOUGH_SEATS.toException();
        }
        // 随机选择
        List<SeatReservationEO> selectedSeats = seatReservationEOs.stream()
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                    Collections.shuffle(list);
                    return list.stream().limit(reserveSeatDTO.getTicketCount()).toList();
                }));
        //修改座位预订状态
        for (SeatReservationEO seatReservationEO : selectedSeats) {
            seatReservationEO.setStatus(ENABLE);
            seatReservationMapper.updateById(seatReservationEO);
        }
        //过滤符合条件的EO，收录座位id
        List<Long> seatIdList = selectedSeats.stream()
                .map(SeatReservationEO::getId)
                .toList();

        return  ReserveSeatResultDTO.builder()
                .seatReservationIds(seatIdList)
                .startTime(startStationTime)
                .endTime(endStationTime)
                .build();
    }

    /**
     * 批量更新座位预约状态
     *
     * @param seatReservationChangeStatusREQ 批量更新座位预约状态参数
     */
    @Override
    public void batchUpdateStatus(SeatReservationChangeStatusREQ seatReservationChangeStatusREQ) {
        // 参数校验
        if (seatReservationChangeStatusREQ == null ||
            seatReservationChangeStatusREQ.getIds() == null ||
            seatReservationChangeStatusREQ.getIds().isEmpty() ||
            seatReservationChangeStatusREQ.getStatus() == null) {
            throw THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }

        // 批量更新座位预约状态
        List<SeatReservationEO> seatReservationEOs = seatReservationMapper.selectBatchIds(seatReservationChangeStatusREQ.getIds());
        if (CollUtil.isEmpty(seatReservationEOs)) {
            return;
        }

        // 设置新的状态
        seatReservationEOs.forEach(seatReservationEO -> {
            seatReservationEO.setStatus(seatReservationChangeStatusREQ.getStatus());
        });

        // 批量更新数据库
        boolean updateResult = self.updateBatchById(seatReservationEOs);
        if (!updateResult) {
            throw MODIFICATION_FAILED.toException();
        }
    }

    /**
     * 根据班次ID获取座位预约ID列表
     *
     * @param scheduleId 班次ID
     * @return 座位预约ID列表
     */
    @Override
    public List<Long> getSeatReservationIdsByScheduleId(Long scheduleId) {
        if (scheduleId == null) {
            return List.of();
        }
        LambdaQueryWrapper<SeatReservationEO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SeatReservationEO::getScheduleId, scheduleId);
        return seatReservationMapper.selectList(queryWrapper).stream().map(SeatReservationEO::getId).toList();
    }

}
