package top.flowerstardream.atbs.order.biz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.flowerstardream.atbs.order.ao.req.TicketPageQueryREQ;
import top.flowerstardream.atbs.order.ao.req.TicketStatusChangeREQ;
import top.flowerstardream.atbs.order.ao.res.TicketRES;
import top.flowerstardream.atbs.order.bo.dto.CancelTicketDTO;
import top.flowerstardream.atbs.order.bo.dto.TicketDTO;
import top.flowerstardream.atbs.order.bo.dto.TicketSeatReservationDTO;
import top.flowerstardream.atbs.order.bo.eo.TicketEO;
import top.flowerstardream.atbs.order.common.enums.TicketStatus;
import top.flowerstardream.base.ao.res.BaseStatusRES;
import top.flowerstardream.base.result.PageResult;

import java.util.List;
import java.math.BigDecimal;

/**
 * @Author: 花海
 * @Date: 2024/11/11
 * @Description: 机票服务接口
 */
public interface ITicketService extends IService<TicketEO> {

    /**
     * 新增机票
     * @param ticketDTO 机票请求信息
     */
    void createTickets(TicketDTO ticketDTO);

    /**
     * 根据用户ID查询机票列表
     * @param userId 用户ID
     * @return 机票响应列表
     */
    List<TicketRES> getTicketsByUserId(Long userId);

    /**
     * 分页查询机票列表（后管端）
     * @param req 查询条件
     * @return 分页结果
     */
    PageResult<TicketRES> pageQuery(TicketPageQueryREQ req);

    /**
     * 更新机票状态
     * @param req 状态变更请求
     */
    void updateTicketStatus(TicketStatusChangeREQ req);

    /**
     * 取消机票
     * @param ticketId 机票ID
     * @param userId 用户ID
     */
    void cancelTicket(Long ticketId, Long userId);

    /**
     * 根据订单ID查询机票列表
     * @param orderId 订单ID
     * @return 机票响应列表
     */
    List<TicketRES> getTicketsByOrderId(Long orderId);

    /**
     * 根据订单信息取消机票
     * @param cancelTicketDTO 订单信息
     */
    void cancelTicketByOrder(CancelTicketDTO cancelTicketDTO);

    /**
     * 根据座位预订ID查询机票列表
     * @param seatReservationId 座位预订ID
     * @return 机票响应列表
     */
    List<TicketSeatReservationDTO> getTickets(Long seatReservationId);

    /**
     * 获取机票状态列表
     * @return 机票状态列表
     */
    List<BaseStatusRES<TicketStatus>> getStatus();

    /**
     * 根据机票ID查询机票信息
     * @param id 机票ID
     * @return 机票信息
     */
    TicketRES getByTicketId(Long id);

    List<TicketRES> getByOrderId(Long id);

    /**
     * 获取某日乘客数
     * @param date 日期
     * @return 乘客数
     */
    BigDecimal getDailyPassengers(String date);
}