package top.flowerstardream.atbs.airplane.biz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.flowerstardream.atbs.airplane.bo.dto.ReserveSeatDTO;
import top.flowerstardream.atbs.airplane.bo.dto.ReserveSeatResultDTO;
import top.flowerstardream.atbs.airplane.bo.dto.SeatReservationDTO;
import top.flowerstardream.atbs.airplane.ao.req.SeatReservationChangeStatusREQ;
import top.flowerstardream.atbs.airplane.ao.req.SeatReservationREQ;
import top.flowerstardream.atbs.airplane.ao.res.SeatReservationRES;
import top.flowerstardream.atbs.airplane.ao.pqreq.SeatReservationPageQueryREQ;
import top.flowerstardream.atbs.airplane.bo.eo.SeatReservationEO;
import top.flowerstardream.base.ao.res.BaseStatusRES;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.service.IBaseService;
import top.flowerstardream.base.state.BaseStatus;

import java.util.List;

/**
 * @Author: QAQ
 * @Date: 2025/11/10 16:01
 * @Description: 座位预约服务
 */
public interface ISeatReservationService extends IBaseService<SeatReservationEO> {

    /**
     * 新增座位预约
     * @param seatReservationREQ
     */
    void addSeatReservation(SeatReservationREQ seatReservationREQ);

    /**
     * 删除座位预约
     * @param ids
     */
    void deleteSeatReservation(List<Long> ids);

    /**
     * 修改座位预约
     * @param seatReservationREQ
     */
    void updateSeatReservation(SeatReservationREQ seatReservationREQ);

    /**
     * 分页查询座位预约列表（后管）
     *
     * @param seatReservationPageQueryREQ 座位预约查询条件
     * @return 座位预约查询分页结果
     */
    PageResult<SeatReservationRES> seatReservationPageQuery(SeatReservationPageQueryREQ seatReservationPageQueryREQ);

    /**
     * 根据座位预约ID列表获取座位预约列表
     *
     * @param seatReservationIds 座位预约ID列表
     * @return 座位预约列表
     */
    List<SeatReservationDTO> getSeatReservationByIds(List<Long> seatReservationIds);

    /**
     * 释放座位
     *
     * @param seatReservationIds 座位预约ID列表
     */
    void releaseSeat(List<Long> seatReservationIds);

    /**
     * 预订座位
     *
     * @param reserveSeatDTO 预订座位参数
     * @return 座位预约结果
     */
    ReserveSeatResultDTO reserveSeat(ReserveSeatDTO reserveSeatDTO);

    List<BaseStatusRES<BaseStatus>> getStatus();

    /**
     * 批量更新座位预约状态
     *
     * @param seatReservationChangeStatusREQ 批量更新座位预约状态参数
     */
    void batchUpdateStatus(SeatReservationChangeStatusREQ seatReservationChangeStatusREQ);

    /**
     * 根据班次ID获取座位预约ID列表
     *
     * @param scheduleId 班次ID
     * @return 座位预约ID列表
     */
    List<Long> getSeatReservationIdsByScheduleId(Long scheduleId);
}
