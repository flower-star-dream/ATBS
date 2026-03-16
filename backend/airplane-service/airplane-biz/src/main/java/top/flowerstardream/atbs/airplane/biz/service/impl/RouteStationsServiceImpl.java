package top.flowerstardream.atbs.airplane.biz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.flowerstardream.atbs.airplane.bo.dto.CalcTicketPriceDTO;
import top.flowerstardream.atbs.airplane.ao.req.RouteREQ;
import top.flowerstardream.atbs.airplane.ao.req.RouteStationsREQ;
import top.flowerstardream.atbs.airplane.ao.req.SortREQ;
import top.flowerstardream.atbs.airplane.ao.res.RouteStationsRES;
import top.flowerstardream.atbs.airplane.biz.mapper.RouteMapper;
import top.flowerstardream.atbs.airplane.biz.mapper.StationMapper;
import top.flowerstardream.atbs.airplane.biz.service.IRouteService;
import top.flowerstardream.atbs.airplane.biz.tool.Calculation;
import top.flowerstardream.atbs.airplane.bo.eo.RouteEO;
import top.flowerstardream.atbs.airplane.bo.eo.RouteStationsEO;
import top.flowerstardream.atbs.airplane.ao.pqreq.RouteStationsPageQueryREQ;
import top.flowerstardream.atbs.airplane.biz.mapper.RouteStationsMapper;
import top.flowerstardream.atbs.airplane.biz.service.IRouteStationsService;
import top.flowerstardream.atbs.airplane.bo.eo.StationEO;
import top.flowerstardream.base.result.PageResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static top.flowerstardream.atbs.airplane.common.AirplaneExceptionEnum.*;
import static top.flowerstardream.base.exception.BaseExceptionEnum.*;
import static top.flowerstardream.base.exception.ExceptionEnum.*;

@Slf4j
@Service
public class RouteStationsServiceImpl extends ServiceImpl<RouteStationsMapper, RouteStationsEO> implements IRouteStationsService {

    @Resource
    private RouteStationsMapper routeStationsMapper;

    @Resource
    private RouteMapper routeMapper;

    @Resource
    private StationMapper stationMapper;

    @Resource
    private IRouteService routeService;

    @Resource
    private Calculation calculation;

    @Lazy
    @Resource
    private IRouteStationsService self;


    @Override
    public void addRouteStations(RouteStationsREQ routeStationsREQ) {
        //参数校验
        if (routeStationsREQ == null) {
            throw THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }

        //判断路线站点存在，存在则中断
        validateRouteStationsIsExist(routeStationsREQ.getRouteId(), routeStationsREQ.getStationId());

        RouteEO routeEO = routeMapper.selectById(routeStationsREQ.getRouteId());
        if (routeEO == null) {
            throw ROUTE_NOT_EXIST.toException();
        }
        if (routeStationsREQ.getInit() &&
                (routeStationsREQ.getStationSorting() <= 1 ||
                        routeStationsREQ.getStationSorting() > routeEO.getStationCount())) {
            throw INCORRECT_SORTING.toException();
        }

        RouteStationsEO routeStationsEO = new RouteStationsEO();
        BeanUtil.copyProperties(routeStationsREQ, routeStationsEO);
        boolean insert = self.save(routeStationsEO);
        if (!insert) {
            throw INSERTION_FAILED.toException();
        }
        if (routeStationsREQ.getInit()) {
            return;
        }

        RouteREQ route = RouteREQ.builder()
                .id(routeStationsREQ.getRouteId())
                .stationCount(routeEO.getStationCount() + 1)
                .build();
        routeService.updateRoute(route);
    }


    @Override
    @Transactional
    public void deleteRouteStations(List<Long> ids) {
        if(CollUtil.isEmpty(ids)){
            return;
        }

        // 查询要删除的路线站点信息，用于后续更新排序
        List<RouteStationsEO> toDeleteStations = routeStationsMapper.selectBatchIds(ids);
        if (CollUtil.isEmpty(toDeleteStations)) {
            return;
        }

        // 批量删除路线站点
        boolean delete = self.removeByIds(ids);
        if (!delete) {
            throw DELETION_FAILED.toException();
        }

        // 按路线分组，重新排序剩余站点并更新路线站点总数
        Map<Long, List<RouteStationsEO>> routeStationsMap = toDeleteStations.stream()
                .collect(Collectors.groupingBy(RouteStationsEO::getRouteId));

        for (Map.Entry<Long, List<RouteStationsEO>> entry : routeStationsMap.entrySet()) {
            Long routeId = entry.getKey();
            List<RouteStationsEO> deletedStationsInRoute = entry.getValue();

            // 获取该路线剩余的所有站点
            LambdaQueryWrapper<RouteStationsEO> queryWrapper = Wrappers.lambdaQuery();
            queryWrapper.eq(RouteStationsEO::getRouteId, routeId);
            queryWrapper.orderByAsc(RouteStationsEO::getStationSorting);
            List<RouteStationsEO> remainingStations = routeStationsMapper.selectList(queryWrapper);

            // 重新排序
            for (int i = 0; i < remainingStations.size(); i++) {
                RouteStationsEO station = remainingStations.get(i);
                station.setStationSorting(i + 1);
                routeStationsMapper.updateById(station);
            }

            // 更新路线的站点总数
            RouteEO routeEO = routeMapper.selectById(routeId);
            if (routeEO != null) {
                RouteREQ routeREQ = RouteREQ.builder()
                        .id(routeId)
                        .stationCount(routeEO.getStationCount() - deletedStationsInRoute.size())
                        .build();
                routeService.updateRoute(routeREQ);
            }
        }
    }

    @Override
    public void updateRouteStations(RouteStationsREQ routeStationsREQ) {
        //参数校验
        if (routeStationsREQ == null) {
            throw THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }
        RouteStationsEO routeStationsEO = new RouteStationsEO();
        BeanUtil.copyProperties(routeStationsREQ, routeStationsEO);
        boolean update = self.updateById(routeStationsEO);
        if (!update) {
            throw MODIFICATION_FAILED.toException();
        }
    }

    @Override
    public PageResult<RouteStationsRES> routeStationsPageQuery(RouteStationsPageQueryREQ routeStationsPageQueryREQ) {
        //参数校验
        if (routeStationsPageQueryREQ == null) {
            throw THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }
        //设置分页参数默认值
        if (routeStationsPageQueryREQ.getPage() <= 0) {
            routeStationsPageQueryREQ.setPage(1);
        }
        if (routeStationsPageQueryREQ.getPageSize() <= 0) {
            routeStationsPageQueryREQ.setPageSize(10);
        }

        //创建分页对象
        Page<RouteStationsEO> page = new Page<>(routeStationsPageQueryREQ.getPage(), routeStationsPageQueryREQ.getPageSize());

        //创建查询条件
        LambdaQueryWrapper<RouteStationsEO> queryWrapper = Wrappers.lambdaQuery();
        if (routeStationsPageQueryREQ.getRouteId() != null) {
            queryWrapper.eq(RouteStationsEO::getRouteId, routeStationsPageQueryREQ.getRouteId());
        }
        if (routeStationsPageQueryREQ.getStationId() != null) {
            queryWrapper.eq(RouteStationsEO::getStationId, routeStationsPageQueryREQ.getStationId());
        }
        if (routeStationsPageQueryREQ.getStationSorting() != null) {
            queryWrapper.like(RouteStationsEO::getStationSorting, routeStationsPageQueryREQ.getStationSorting());
        }

        //执行分页查询
        Page<RouteStationsEO> routeStationsResult = routeStationsMapper.selectPage(page, queryWrapper);

        List<RouteStationsEO> records = routeStationsResult.getRecords();
        // 提取所有路线ID和站点ID
        List<Long> recordsRouteIds = records.stream()
                .map(RouteStationsEO::getRouteId)
                .distinct()
                .toList();
        List<Long> recordsStationIds = records.stream()
                .map(RouteStationsEO::getStationId)
                .distinct()
                .toList();

        // 批量查询路线和站点信息
        List<RouteEO> routeList = recordsRouteIds.isEmpty() ? CollUtil.newArrayList() : routeMapper.selectBatchIds(recordsRouteIds);
        List<StationEO> stationList = recordsStationIds.isEmpty() ? CollUtil.newArrayList() : stationMapper.selectBatchIds(recordsStationIds);

        // 构建Map便于快速查找
        Map<Long, String> routeNameMap = routeList.stream()
                .collect(Collectors.toMap(RouteEO::getId, RouteEO::getRouteName));
        Map<Long, String> stationNameMap = stationList.stream()
                .collect(Collectors.toMap(StationEO::getId, StationEO::getStationName));

        // 转换为RES对象并设置名称属性
        List<RouteStationsRES> routeStationsRESList = records.stream()
            .map(record -> convertToRES(record, routeNameMap, stationNameMap))
            .toList();
        //封装返回结果
        PageResult<RouteStationsRES> pageResult = new PageResult<>();
        pageResult.setTotal(pageResult.getTotal());
        pageResult.setRecords(routeStationsRESList);
        return pageResult;

    }

    /*
    * 查询路线站点
    * 校验路线站点是否存在
    * add方法在使用
    */
    private RouteStationsEO getRouteStationsEO(Long routeId, Long stationId) {
        LambdaQueryWrapper<RouteStationsEO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteStationsEO::getRouteId, routeId)
                .eq(RouteStationsEO::getStationId, stationId);
        return routeStationsMapper.selectOne(queryWrapper);
    }

    private void validateRouteStationsIsExist(Long routeId, Long stationId) {

        RouteStationsEO routeStationsEO = getRouteStationsEO(routeId, stationId);
        if(routeStationsEO != null){
            throw ROUTE_STATIONS_ALREADY_EXISTS.toException();
        }
    }

    /**
     * 外部调用
    * */
    @Override
    public BigDecimal calcTicketPrice(CalcTicketPriceDTO calcTicketPriceDTO) {
        //参数校验
        if (calcTicketPriceDTO == null) {
            throw THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }

        return calculation.ticketPriceCalculation(calcTicketPriceDTO);
    }

    /**
     * 根据id排序路线站点
     *
     * @param sortREQ
     */
    @Override
    @Transactional
    public void sort(SortREQ sortREQ) {
        // 参数校验
        if (sortREQ == null || CollUtil.isEmpty(sortREQ.getRouteStationsIds())) {
            throw THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }

        List<Long> ids = sortREQ.getRouteStationsIds();

        // 批量查询需要更新的路线站点信息
        List<RouteStationsEO> routeStationsList = routeStationsMapper.selectBatchIds(ids);

        if (CollUtil.isEmpty(routeStationsList)) {
            return;
        }

        // 按照传入的ID顺序设置新的排序值
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            RouteStationsEO routeStationsEO = routeStationsList.stream()
                    .filter(item -> item.getId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (routeStationsEO != null) {
                routeStationsEO.setStationSorting(i + 1);
                routeStationsMapper.updateById(routeStationsEO);
            }
        }
    }

    private RouteStationsRES convertToRES(RouteStationsEO routeStationsEO,
                                     Map<Long, String> routeNameMap,
                                     Map<Long, String> stationNameMap) {
        RouteStationsRES res = new RouteStationsRES();
        BeanUtil.copyProperties(routeStationsEO, res);
        res.setRouteName(routeNameMap.get(routeStationsEO.getRouteId()));
        res.setStationName(stationNameMap.get(routeStationsEO.getStationId()));
        return res;
    }

}
