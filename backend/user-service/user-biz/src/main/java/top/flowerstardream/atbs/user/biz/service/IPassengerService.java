package top.flowerstardream.atbs.user.biz.service;



import com.baomidou.mybatisplus.extension.service.IService;
import top.flowerstardream.atbs.user.ao.req.PassengerPageQueryREQ;
import top.flowerstardream.atbs.user.ao.req.PassengerREQ;
import top.flowerstardream.atbs.user.ao.res.PassengerRES;
import top.flowerstardream.atbs.user.bo.dto.PassengerDTO;
import top.flowerstardream.atbs.user.bo.eo.PassengerEO;
import top.flowerstardream.base.result.PageResult;

import java.util.List;

/**
 * @Author: 花海
 * @Date: 2025-11-10
 * @Description: 乘客服务接口
 */
public interface IPassengerService extends IService<PassengerEO> {

    /**
     * 分页查询乘客列表
     *
     * @param queryREQ 查询条件
     * @return 乘客列表分页结果
     */
    PageResult<PassengerEO> pageQuery(PassengerPageQueryREQ queryREQ);

    /**
     * 根据ID查询乘客详情
     *
     * @param id 乘客ID
     * @return 乘客详情
     */
    PassengerEO query(Long id);

    /**
     * 获取当前用户关联的乘客列表
     *
     * @param userId 用户ID
     * @return 乘客列表
     */
    List<PassengerRES> getUserPassengers(Long userId);

    /**
     * 新增乘客
     *
     * @param userId 用户ID
     * @param passengerREQ 乘客信息
     */
    void addPassenger(Long userId, PassengerREQ passengerREQ);

    /**
     * 设置默认乘客
     *
     * @param userId 用户ID
     * @param passengerId 乘客ID
     */
    void setDefaultPassenger(Long userId, Long passengerId);

    /**
     * 根据乘客姓名获取乘客列表
     *
     * @param passengerName 乘客姓名
     * @return 乘客列表
     */
    List<Long> getPassengersByName(String passengerName);

    /**
     * 根据乘客ID列表获取乘客信息列表
     *
     * @param passengerIds 乘客ID列表
     * @return 乘客信息列表
     */
    List<PassengerDTO> getPassengersByIds(List<Long> passengerIds);
}
