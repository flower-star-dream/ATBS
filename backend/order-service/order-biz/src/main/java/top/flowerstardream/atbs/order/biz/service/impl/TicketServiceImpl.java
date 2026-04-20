package top.flowerstardream.atbs.order.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.flowerstardream.atbs.order.ao.req.TicketPageQueryREQ;
import top.flowerstardream.atbs.order.ao.req.TicketStatusChangeREQ;
import top.flowerstardream.atbs.order.ao.res.OrderRES;
import top.flowerstardream.atbs.order.ao.res.TicketRES;
import top.flowerstardream.atbs.order.biz.client.AirplaneClient;
import top.flowerstardream.atbs.order.biz.client.UserClient;
import top.flowerstardream.atbs.order.biz.mapper.TicketMapper;
import top.flowerstardream.atbs.order.biz.service.IOrderService;
import top.flowerstardream.atbs.order.biz.service.ITicketService;
import top.flowerstardream.atbs.order.bo.dto.*;
import top.flowerstardream.atbs.order.bo.eo.OrderEO;
import top.flowerstardream.atbs.order.bo.eo.TicketEO;
import top.flowerstardream.atbs.order.common.enums.OrderEvent;
import top.flowerstardream.atbs.order.common.enums.OrderStatus;
import top.flowerstardream.atbs.order.common.enums.TicketEvent;
import top.flowerstardream.atbs.order.common.enums.TicketStatus;
import top.flowerstardream.base.annotation.AutoStateMachine;
import top.flowerstardream.base.annotation.RedissonLock;
import top.flowerstardream.base.ao.res.BaseStatusRES;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.state.StateMachine;
import top.flowerstardream.base.utils.StateRouteParams;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static top.flowerstardream.atbs.order.common.enums.OrderStatus.*;
import static top.flowerstardream.atbs.order.common.enums.TicketExceptionEnum.*;
import static top.flowerstardream.atbs.order.common.enums.TicketStatus.*;
import static top.flowerstardream.atbs.order.common.enums.TicketEvent.*;
import static top.flowerstardream.base.exception.BaseExceptionEnum.*;

/**
 * @Author: 花海
 * @Date: 2024/11/11
 * @Description: 机票服务实现类
 */
@Service
@Slf4j
public class TicketServiceImpl extends ServiceImpl<TicketMapper, TicketEO> implements ITicketService {

    @Resource
    private TicketMapper ticketMapper;

    @Resource
    private UserClient userClient;

    @Resource
    private IOrderService orderService;

    @Resource
    private AirplaneClient airplaneClient;

    @AutoStateMachine
    private StateMachine<TicketStatus, TicketEvent, TicketEO> fsm;

    @Resource
    private ITicketService self;

    /**
     * 新增机票
     * @param ticketDTO 机票请求信息
     */
    @Override
    @RedissonLock(key = "'ticket:create:' + #ticketDTO.getOrderId()", waitTime = 5, leaseTime = 30)
    @Transactional(rollbackFor = Exception.class)
    public void createTickets(TicketDTO ticketDTO) {
        // 1. 调用列车服务查询余票并预订座位
        ReserveSeatDTO reserveSeatDTO = ReserveSeatDTO.builder()
                .scheduleId(ticketDTO.getScheduleId())
                .startStationId(ticketDTO.getStartStationId())
                .endStationId(ticketDTO.getEndStationId())
                .ticketCount(ticketDTO.getPassengerIds().size())
                .build();
        ReserveSeatResultDTO reserveSeatResultDTO = airplaneClient.reserveSeat(reserveSeatDTO).getData();
        List<Long> seatReservationIds = reserveSeatResultDTO.getSeatReservationIds();
        // 2. 校验数据一致性
        if (ticketDTO.getPassengerIds().size() != seatReservationIds.size()) {
            throw SEAT_RESERVATION_FAILED.toException();
        }
        seatReservationIds.forEach(seatReservationId -> {
            if (seatReservationId == null || seatReservationId <= 0) {
                // 余票不足，抛出异常
                throw TICKET_INSUFFICIENT.toException();
            }
        });
        // 3. 为每个乘车人创建机票
        List<TicketEO> ticketList = new ArrayList<>();
        List<Long> passengerIds = ticketDTO.getPassengerIds();
        for (int i = 0; i < passengerIds.size(); i++) {
            TicketEO ticketEO = TicketEO.builder()
                    .orderId(ticketDTO.getOrderId())
                    .passengerId(passengerIds.get(i))
                    .seatReservationId(seatReservationIds.get(i))
                    .status(NORMAL)
                    .money(ticketDTO.getMoney())
                    .startTime(reserveSeatResultDTO.getStartTime())
                    .endTime(reserveSeatResultDTO.getEndTime())
                    .startStationId(ticketDTO.getStartStationId())
                    .endStationId(ticketDTO.getEndStationId())
                    .build();
            ticketList.add(ticketEO);
        }
        // 批量插入机票
        if (!self.saveBatch(ticketList)){
            throw SEAT_RESERVATION_FAILED.toException();
        }
    }

    /**
     * 根据用户ID查询机票列表
     * @param userId 用户ID
     * @return 机票响应列表
     */
    @Override
    public List<TicketRES> getTicketsByUserId(Long userId) {
        // 1. 调用订单服务获取该用户的所有订单ID
        List<OrderRES> orders = orderService.getUserOrders(userId);
        List<Long> orderIds = orders.stream().map(OrderRES::getId).toList();
        if (orderIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 根据订单ID列表查询机票
        LambdaQueryWrapper<TicketEO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.in(TicketEO::getOrderId, orderIds);
        List<TicketEO> ticketList = ticketMapper.selectList(queryWrapper);

        // 转换为响应对象并填充信息
        return convertToRES(ticketList);
    }

    /**
     * 分页查询机票列表（后管端）
     * @param req 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<TicketRES> pageQuery(TicketPageQueryREQ req) {
        if (req == null) {
            throw THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }
        if (req.getPage() <= 0) {
            req.setPage(1);
        }
        if (req.getPageSize() <= 0) {
            req.setPageSize(10);
        }
        Page<TicketEO> page = new Page<>(req.getPage(), req.getPageSize());
        LambdaQueryWrapper<TicketEO> queryWrapper = Wrappers.lambdaQuery();
        
        // 构建查询条件
        if (req.getOrderId() != null) {
            queryWrapper.eq(TicketEO::getOrderId, req.getOrderId());
        }
        if (req.getScheduleId() != null) {
            List<Long> seatReservationIds = airplaneClient.getSeatReservationIdsByScheduleId(req.getScheduleId()).getData();
            if (CollUtil.isNotEmpty(seatReservationIds)) {
                queryWrapper.in(TicketEO::getSeatReservationId, seatReservationIds);
            } else {
                queryWrapper.eq(TicketEO::getSeatReservationId, -1L);
            }
        }
        // 根据乘车人姓名查询
        if (StringUtils.isNotBlank((req.getPassengerName()))) {
            List<Long> passengerIds = userClient.getPassengerIdsByName(req.getPassengerName()).getData();
            if (CollUtil.isNotEmpty(passengerIds)) {
                queryWrapper.in(TicketEO::getPassengerId, passengerIds);
            } else {
                queryWrapper.eq(TicketEO::getPassengerId, -1L);
            }
        }
        if (StringUtils.isNotBlank((req.getStartStation()))) {
            List<Long> startStationIds = airplaneClient.getStationIdsByName(req.getStartStation()).getData();
            if (CollUtil.isNotEmpty(startStationIds)) {
                queryWrapper.in(TicketEO::getStartStationId, startStationIds);
            } else {
                queryWrapper.eq(TicketEO::getStartStationId, -1L);
            }
        }
        if (StringUtils.isNotBlank((req.getEndStation()))) {
            List<Long> endStationIds = airplaneClient.getStationIdsByName(req.getEndStation()).getData();
            if (CollUtil.isNotEmpty(endStationIds)) {
                queryWrapper.in(TicketEO::getEndStationId, endStationIds);
            } else {
                queryWrapper.eq(TicketEO::getEndStationId, -1L);
            }
        }
        if (ObjectUtil.isNotEmpty((req.getRideDateStart()))) {
            queryWrapper.ge(TicketEO::getStartTime, req.getRideDateStart());
        }
        if (ObjectUtil.isNotEmpty(req.getRideDateEnd())) {
            queryWrapper.le(TicketEO::getEndTime, req.getRideDateEnd());
        }
        
        IPage<TicketEO> result = ticketMapper.selectPage(page, queryWrapper);
        List<TicketRES> ticketRESList = convertToRES(result.getRecords());
        return new PageResult<>(result.getTotal(), ticketRESList);
    }

    /**
     * 更新机票状态
     * @param req 状态变更请求
     */
    @Override
    @RedissonLock(key = "'ticket:status:' + #req.getId()", waitTime = 3, leaseTime = 30)
    @GlobalTransactional(rollbackFor = Exception.class)
    public void updateTicketStatus(TicketStatusChangeREQ req) {
        if (req == null) {
            throw PARAM_ERROR.toException();
        }
        TicketEO oldTicket = ticketMapper.selectById(req.getId());
        if (oldTicket == null) {
            throw TICKET_NOT_EXIST.toException();
        }
        if (USED.equals(oldTicket.getStatus())) {
            throw TICKET_ALREADY_USED.toException();
        }
        StateRouteParams params = StateRouteParams.create()
                .addParam("oldTicket", oldTicket);
        switch (req.getStatus()) {
            case USED:
                fsm.fire(oldTicket.getStatus(), USE, params);
                break;
            case CHANGED:
                params.addParam("req", req);
                fsm.fire(oldTicket.getStatus(), CHANGE, params);
                break;
            case CANCELLED:
                fsm.fire(oldTicket.getStatus(), CANCEL, params);
                break;
            case REFUNDED:
                fsm.fire(oldTicket.getStatus(), REFUND_TICKET, params);
                break;
        }
    }

    /**
     * 取消机票
     * @param ticketId 机票ID
     * @param userId 用户ID
     */
    @Override
    @RedissonLock(key = "'ticket:cancel:' + #ticketId", waitTime = 5, leaseTime = 30)
    @Transactional(rollbackFor = Exception.class)
    public void cancelTicket(Long ticketId, Long userId) {
        // 验证机票归属
        TicketEO ticketEO = ticketMapper.selectById(ticketId);
        if (ticketEO == null) {
            throw TICKET_NOT_EXIST.toException();
        }
        
        // 通过验证订单归属
        boolean isOwner = ticketEO.getOrderId().equals(userId);
        if (!isOwner) {
            throw TICKET_PERMISSION_DENIED.toException();
        }

        if (!NORMAL.equals(ticketEO.getStatus())) {
            throw TICKET_STATUS_NOT_ALLOWED.toException();
        }

        // 调用订单服务访问订单状态
        OrderStatus orderStatus = orderService.getOrderStatus(ticketEO.getOrderId());
        if (orderStatus.getCode() > TICKETED.getCode()) {
            throw ORDER_STATUS_NOT_ALLOWED.toException();
        }
        // 更新状态为已取消
        TicketStatusChangeREQ ticketStatusChangeREQ = new TicketStatusChangeREQ();
        ticketStatusChangeREQ.setId(ticketId);
        if (orderStatus.equals(PENDING_PAY)) {
            ticketStatusChangeREQ.setStatus(TicketStatus.CANCELLED);
        } else {
            ticketStatusChangeREQ.setStatus(TicketStatus.REFUNDED);
        }
        updateTicketStatus(ticketStatusChangeREQ);
    }

    /**
     * 根据订单ID查询机票列表
     *
     * @param orderId 订单ID
     * @return 机票响应列表
     */
    @Override
    public List<TicketRES> getTicketsByOrderId(Long orderId) {
        if (orderId == null) {
            throw PARAM_ERROR.toException();
        }
        // 根据订单ID查询机票列表
        LambdaQueryWrapper<TicketEO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(TicketEO::getOrderId, orderId);
        List<TicketEO> ticketList = ticketMapper.selectList(queryWrapper);

        // 转换为响应对象并填充信息
        return convertToRES(ticketList);
    }

    /**
     * 根据订单信息取消机票
     *
     * @param cancelTicketDTO  订单信息
     */
    @Override
    @RedissonLock(key = "'ticket:cancelByOrder:' + #cancelTicketDTO.getOrderId()", waitTime = 5, leaseTime = 30)
    @Transactional(rollbackFor = Exception.class)
    public void cancelTicketByOrder(CancelTicketDTO cancelTicketDTO) {
        LambdaQueryWrapper<TicketEO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(TicketEO::getOrderId, cancelTicketDTO.getOrderId());
        List<TicketEO> ticketList = ticketMapper.selectList(queryWrapper);
        List<Long> seatReservationIds = ticketList.stream()
                                                .map(TicketEO::getSeatReservationId)
                                                .filter(Objects::nonNull)
                                                .distinct()
                                                .toList();
        if (CollUtil.isNotEmpty(seatReservationIds)) {
            airplaneClient.releaseSeat(seatReservationIds);
            // 更新机票状态
            if (Objects.equals(cancelTicketDTO.getStatus(), OrderStatus.CANCELLED)) {
                ticketList.forEach(ticket -> ticket.setStatus(TicketStatus.CANCELLED));
            } else {
                ticketList.forEach(ticket -> ticket.setStatus(TicketStatus.REFUNDED));
            }
            self.updateBatchById(ticketList);
        }
    }

    /**
     * 将EO转换为RES
     * @param ticketList 机票实体列表对象
     * @return 机票响应对象
     */
    private List<TicketRES> convertToRES(List<TicketEO> ticketList) {
        if (CollUtil.isEmpty(ticketList)) {
            return Collections.emptyList();
        }

        /* 1. 一次性查所有关联数据 */
        // 1.1 乘车人
        List<Long> passengerIds = ticketList.stream()
                                            .map(TicketEO::getPassengerId)
                                            .distinct()
                                            .toList();
        Map<Long, PassengerDTO> passengerMap = userClient.getPassengerByIds(passengerIds)
                                                         .getData()
                                                         .stream()
                                                         .collect(Collectors.toMap(PassengerDTO::getId, Function.identity()));

        // 1.2 座位
        List<Long> seatIds = ticketList.stream()
                                          .map(TicketEO::getSeatReservationId)
                                          .distinct()
                                          .toList();
        Map<Long, SeatReservationDTO> seatNumMap = airplaneClient.getSeatReservationByIds(seatIds)
                                                            .getData()
                                                            .stream()
                                                            .collect(Collectors.toMap(SeatReservationDTO::getId, Function.identity()));

        // 1.3 站点
        List<Long> stationIds = Stream.concat(
                                        ticketList.stream().map(TicketEO::getStartStationId),
                                        ticketList.stream().map(TicketEO::getEndStationId))
                                      .distinct()
                                      .toList();
        Map<Long, StationsDTO> stationNameMap = airplaneClient.getStationNamesByStationIds(stationIds)
                                                            .getData()
                                                            .stream()
                                                            .collect(Collectors.toMap(StationsDTO::getId, Function.identity()));

        /* 2. 组装结果 */
        return ticketList.stream()
                         .map(ticket -> {
                             TicketRES res = new TicketRES();
                             BeanUtils.copyProperties(ticket, res);

                             // 2.1 乘车人
                             PassengerDTO passenger = passengerMap.get(ticket.getPassengerId());
                             if (passenger != null) {
                                 res.setRealName(passenger.getRealName());
                                 res.setIdCard(passenger.getIdCard());
                                 res.setCardType(passenger.getCardType());
                             }

                             // 2.2 座位号
                             SeatReservationDTO seat = seatNumMap.get(ticket.getSeatReservationId());
                             res.setSeatNumber(seat.getSeatNum());
                             res.setScheduleId(seat.getScheduleId());

                             // 2.3 起终站
                             StationsDTO startStation = stationNameMap.get(ticket.getStartStationId());
                             StationsDTO endStation = stationNameMap.get(ticket.getEndStationId());
                             res.setStartStationId(startStation.getId());
                             res.setStartStation(startStation.getName());
                             res.setEndStationId(endStation.getId());
                             res.setEndStation(endStation.getName());

                             return res;
                         })
                         .toList();
    }

    @Override
    public List<TicketSeatReservationDTO> getTickets(Long seatReservationId) {
        //参数校验
        if (seatReservationId == null || seatReservationId <= 0) {
            throw PARAM_ERROR.toException();
        }
        LambdaQueryWrapper<TicketEO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(TicketEO::getSeatReservationId, seatReservationId);
        //将EO转换为DTO
        List<TicketEO> ticketList = ticketMapper.selectList(queryWrapper);

        return ticketList.stream()
                .map(ticket -> {
                    TicketSeatReservationDTO dto = new TicketSeatReservationDTO();
                    BeanUtils.copyProperties(ticket, dto);
                    return dto;
                })
                .toList();
    }

    @Override
    public List<BaseStatusRES<TicketStatus>> getStatus() {
        // 使用LambdaQueryWrapper进行分组统计
        List<Map<String, Object>> statusCounts = ticketMapper.count();

        // 将统计结果转换为BaseStatusRES<TicketStatus>列表
        return statusCounts.stream()
            .map(map -> {
                BaseStatusRES<TicketStatus> statusRES = new BaseStatusRES<TicketStatus>();
                statusRES.setStatus((TicketStatus) map.get("state"));
                statusRES.setCount((Integer) map.get("count"));
                statusRES.setDescription(statusRES.getStatus().getName());
                return statusRES;
            })
            .collect(Collectors.toList());
    }

    /**
     * 根据机票ID查询机票信息
     *
     * @param id 机票ID
     * @return 机票信息
     */
    @Override
    public TicketRES getByTicketId(Long id) {
        TicketEO ticketEO = ticketMapper.selectById(id);
        if (ticketEO != null) {
            return convertToRES(Collections.singletonList(ticketEO)).get(0);
        }
        return null;
    }

    /**
     * @param id
     * @return
     */
    @Override
    public List<TicketRES> getByOrderId(Long id) {
        if (id == null || id <= 0) {
            throw PARAM_ERROR.toException();
        }
        LambdaQueryWrapper<TicketEO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(TicketEO::getOrderId, id);
        queryWrapper.orderByAsc(TicketEO::getId);
        List<TicketEO> ticketList = ticketMapper.selectList(queryWrapper);
        return convertToRES(ticketList);
    }

    /**
     * 获取某日乘客数
     * @param date 日期
     * @return 乘客数
     */
    @Override
    public BigDecimal getDailyPassengers(String date){
        if (date == null || date.isEmpty() || !date.matches("yyyy-MM-dd")) {
            throw PARAM_ERROR.toException();
        }
        LocalDateTime startTime = LocalDateTime.parse(date + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime endTime = LocalDateTime.parse(date + " 23:59:59", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Long count = ticketMapper.countByStartTimeBetween(startTime, endTime);
        return new BigDecimal(count/1000.0);
    }
}

