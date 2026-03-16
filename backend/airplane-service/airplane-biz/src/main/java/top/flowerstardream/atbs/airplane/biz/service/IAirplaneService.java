package top.flowerstardream.atbs.airplane.biz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.flowerstardream.atbs.airplane.ao.req.AirplaneREQ;
import top.flowerstardream.atbs.airplane.ao.res.AirplaneRES;
import top.flowerstardream.atbs.airplane.ao.pqreq.AirplanePageQueryREQ;
import top.flowerstardream.atbs.airplane.bo.eo.AirplaneEO;
import top.flowerstardream.base.result.PageResult;


import java.util.List;

/**
 * @Author: QAQ
 * @Date: 2025/11/10 16:01
 * @Description: 飞机服务
 */
public interface IAirplaneService extends IService<AirplaneEO> {

    /**
     * 新增飞机
     * @param airplaneREQ
     */
    void addAirplane(AirplaneREQ airplaneREQ);

    /**
     * 批量删除飞机
     * @param ids
     */
    void deleteAirplane(List<Long> ids);

    /**
     * 修改飞机
     * @param airplaneREQ
     */
    void updateAirplane(AirplaneREQ airplaneREQ);

    /**
     * 分页查询飞机列表（管理）
     *
     * @param airplanePageQueryREQ 飞机查询条件
     * @return 飞机查询分页结果
     */
    PageResult<AirplaneRES> airplanePageQuery(AirplanePageQueryREQ airplanePageQueryREQ);

}
