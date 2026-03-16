package top.flowerstardream.atbs.airplane.biz.service;


import com.baomidou.mybatisplus.extension.service.IService;
import top.flowerstardream.atbs.airplane.bo.dto.CalcTicketPriceDTO;
import top.flowerstardream.atbs.airplane.ao.req.RouteStationsREQ;
import top.flowerstardream.atbs.airplane.ao.req.SortREQ;
import top.flowerstardream.atbs.airplane.ao.res.RouteStationsRES;
import top.flowerstardream.atbs.airplane.bo.eo.RouteStationsEO;
import top.flowerstardream.atbs.airplane.ao.pqreq.RouteStationsPageQueryREQ;
import top.flowerstardream.base.result.PageResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Author: QAQ
 * @Date: 2025/11/09/23:00
 * @Description: 路线站点服务
 */
public interface IRouteStationsService extends IService<RouteStationsEO> {

    /**
     * 新增路线站点
     * @param routeStationsREQ
     */
    void addRouteStations(RouteStationsREQ routeStationsREQ);
    /**
     * 删除路线站点
     * @param ids
     */
    void deleteRouteStations(List<Long> ids);
    /**
     * 修改路线站点
     * @param routeStationsREQ
     */
    void updateRouteStations(RouteStationsREQ routeStationsREQ);
    /**
     * 分页查询路线站点列表（管理）
     * @param routeStationPageQueryREQ 查询条件
     * @return 路线查询分页结果
     */
    PageResult<RouteStationsRES> routeStationsPageQuery(RouteStationsPageQueryREQ routeStationPageQueryREQ);

    /**
     * 计算车票价格
     * @param calcTicketPriceDTO
     * @return
     */
    BigDecimal calcTicketPrice(CalcTicketPriceDTO calcTicketPriceDTO);

    /**
     * 根据id排序路线站点
     * @param sortREQ
     */
    void sort(SortREQ sortREQ);
}
