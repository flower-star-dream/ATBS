package top.flowerstardream.atbs.airplane.biz.service;


import com.baomidou.mybatisplus.extension.service.IService;
import top.flowerstardream.atbs.airplane.ao.req.RouteREQ;
import top.flowerstardream.atbs.airplane.ao.res.RouteRES;
import top.flowerstardream.atbs.airplane.bo.eo.RouteEO;
import top.flowerstardream.atbs.airplane.ao.pqreq.RoutePageQueryREQ;
import top.flowerstardream.base.result.PageResult;


import java.util.List;

/**
 * @Author: QAQ
 * @Date: 2025/11/10 16:01
 * @Description: 路线服务
 */
public interface IRouteService extends IService<RouteEO> {

    /**
     * 新增路线
     * @param routeREQ
     */
    void addRoute(RouteREQ routeREQ);
    /**
     * 批量删除路线
     * @param ids
     */
    void deleteRoute(List<Long> ids);
    /**
     * 修改路线
     * @param routeREQ
     */
    void updateRoute(RouteREQ routeREQ);


    /**
     * 分页查询路线列表（管理）
     *
     * @param routePageQueryREQ 查询条件
     * @return 路线查询分页结果
     */
    PageResult<RouteRES> routePageQuery(RoutePageQueryREQ routePageQueryREQ);

}
