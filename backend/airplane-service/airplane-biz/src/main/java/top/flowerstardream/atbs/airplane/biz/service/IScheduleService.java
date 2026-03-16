package top.flowerstardream.atbs.airplane.biz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.flowerstardream.atbs.airplane.ao.pqreq.RealTimeSchedulePageQueryREQ;
import top.flowerstardream.atbs.airplane.ao.req.RealTimeScheduleREQ;
import top.flowerstardream.atbs.airplane.ao.req.ScheduleREQ;
import top.flowerstardream.atbs.airplane.ao.res.RealTimeScheduleRES;
import top.flowerstardream.atbs.airplane.ao.res.ScheduleRES;
import top.flowerstardream.atbs.airplane.bo.eo.ScheduleEO;
import top.flowerstardream.atbs.airplane.ao.pqreq.SchedulePageQueryREQ;
import top.flowerstardream.base.result.PageResult;

import java.util.List;

/**
 * @Author: QAQ
 * @Date: 2025/11/10 16:01
 * @Description: 班次服务
 */
public interface IScheduleService extends IService<ScheduleEO> {

    /**
     * 新增班次
     * @param scheduleREQ
     */
    void addSchedule(ScheduleREQ scheduleREQ);
    /**
     * 批量删除班次
     * @param ids
     */
    void deleteSchedule(List<Long> ids);
    /**
     * 修改班次
     * @param scheduleREQ
     */
    void updateSchedule(ScheduleREQ scheduleREQ);
    /**
     * 分页查询班次列表（管理）
     *
     * @param schedulePageQueryREQ 查询条件
     * @return 班次查询分页结果
     */
    PageResult<ScheduleRES> schedulePageQuery(SchedulePageQueryREQ schedulePageQueryREQ);


    /**
     * 根据id查询班次
     *
      * @param realTimeScheduleREQ 查询条件
     * @return 班次信息
     */
    RealTimeScheduleRES getSchedule(RealTimeScheduleREQ realTimeScheduleREQ);

    /**
     * 分页查询实时班次列表（小程序）
     *
     * @param realTimeSchedulePageQueryREQ 搜索条件
     * @return 班次查询分页结果
     */
    PageResult<RealTimeScheduleRES> getRealTimeSchedule(RealTimeSchedulePageQueryREQ realTimeSchedulePageQueryREQ);

    /**
     * 查询余票数量
     * @param scheduleId 班次ID
     * @return 余票数量
     */
    Integer getAvailingTickets(Long scheduleId);
}
