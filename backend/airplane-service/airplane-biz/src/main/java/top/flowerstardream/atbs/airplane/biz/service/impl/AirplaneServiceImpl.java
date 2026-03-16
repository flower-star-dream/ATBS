package top.flowerstardream.atbs.airplane.biz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.flowerstardream.atbs.airplane.ao.req.AirplaneREQ;
import top.flowerstardream.atbs.airplane.ao.res.AirplaneRES;
import top.flowerstardream.atbs.airplane.bo.eo.AirplaneEO;
import top.flowerstardream.atbs.airplane.bo.eo.ScheduleEO;
import top.flowerstardream.atbs.airplane.ao.pqreq.AirplanePageQueryREQ;
import top.flowerstardream.atbs.airplane.biz.mapper.ScheduleMapper;
import top.flowerstardream.atbs.airplane.biz.mapper.AirplaneMapper;
import top.flowerstardream.atbs.airplane.biz.service.IAirplaneService;
import top.flowerstardream.base.result.PageResult;

import java.util.List;

import static top.flowerstardream.atbs.airplane.common.AirplaneExceptionEnum.AIRPLANE_ALREADY_EXISTS;
import static top.flowerstardream.atbs.airplane.common.AirplaneExceptionEnum.AIRPLANE_IS_USED;
import static top.flowerstardream.base.exception.BaseExceptionEnum.*;
import static top.flowerstardream.base.exception.ExceptionEnum.*;

@Service
@Slf4j
public class AirplaneServiceImpl extends ServiceImpl<AirplaneMapper, AirplaneEO> implements IAirplaneService {

    @Lazy
    @Resource
    private IAirplaneService self;

    @Resource
    private AirplaneMapper airplaneMapper;

    @Resource
    private ScheduleMapper scheduleMapper;


    @Override
    public void addAirplane(AirplaneREQ airplaneREQ) {
        //参数校验
        if (airplaneREQ == null) {
            throw THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }
        //查询飞机是否存在
        validateAirplaneIsExist(airplaneREQ.getAirplaneName());

        //打包req的属性进入EO
        AirplaneEO airplaneEO = new AirplaneEO();
        BeanUtil.copyProperties(airplaneREQ, airplaneEO);
        boolean insert = self.save(airplaneEO);
        if (!insert) {
            throw INSERTION_FAILED.toException();
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAirplane(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        //排查有无使用该路线
        ids.forEach(id -> {
            //获取飞机信息
            AirplaneEO airplaneEO = self.getById(id);
            if (airplaneEO == null) {
                return;
            }

            LambdaQueryWrapper<ScheduleEO> queryWrapper = Wrappers.lambdaQuery();
            queryWrapper.eq(ScheduleEO::getAirplaneId,id);
            List<ScheduleEO> schedules = scheduleMapper.selectList(queryWrapper);

            if (CollUtil.isNotEmpty(schedules)){
                throw AIRPLANE_IS_USED.toException();
            }
        });


    }

    @Override
    public void updateAirplane(AirplaneREQ airplaneREQ) {
        //参数校验
        if (airplaneREQ == null) {
            throw THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }
        AirplaneEO airplaneEO = new AirplaneEO();
        BeanUtil.copyProperties(airplaneREQ, airplaneEO);
        boolean update = self.updateById(airplaneEO);
        if (!update) {
           throw MODIFICATION_FAILED.toException();
        }

    }

    @Override
    public PageResult<AirplaneRES> airplanePageQuery(AirplanePageQueryREQ airplanePageQueryREQ) {
        //参数校验
        if (airplanePageQueryREQ == null) {
            throw THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }
        //设置分页参数默认值
        if (airplanePageQueryREQ.getPage() <= 0){
            airplanePageQueryREQ.setPage(1);
        }
        if(airplanePageQueryREQ.getPageSize() <= 0){
            airplanePageQueryREQ.setPageSize(10);
        }

        //创建分页对象
        Page<AirplaneEO> page = new Page<>(airplanePageQueryREQ.getPage(), airplanePageQueryREQ.getPageSize());
        //创建查询条件
        LambdaQueryWrapper<AirplaneEO> queryWrapper = new LambdaQueryWrapper<>();

        //查询条件
        if (airplanePageQueryREQ.getId() != null) {
            queryWrapper.eq(AirplaneEO::getId, airplanePageQueryREQ.getId());
        }
        if (StrUtil.isNotBlank(airplanePageQueryREQ.getAirplaneName())) {
            queryWrapper.like(AirplaneEO::getAirplaneName, airplanePageQueryREQ.getAirplaneName());
        }
        if (StrUtil.isNotBlank(airplanePageQueryREQ.getAirplaneModel())) {
            queryWrapper.like(AirplaneEO::getAirplaneModel, airplanePageQueryREQ.getAirplaneModel());
        }
        if (airplanePageQueryREQ.getServiceYears() != null) {
            queryWrapper.like(AirplaneEO::getServiceYears, airplanePageQueryREQ.getServiceYears());
        }

        //执行分页查询
        Page<AirplaneEO> airplaneResult = airplaneMapper.selectPage(page, queryWrapper);

        //将EO转换为RES
        List<AirplaneRES> airplaneList = airplaneResult.getRecords().stream()
                .map(airplaneEO -> {
                    AirplaneRES res = new AirplaneRES();
                    BeanUtil.copyProperties(airplaneEO, res);
                    return res;
                }).toList();

        //封装返回结果
        PageResult<AirplaneRES> pageResult = new PageResult<>();
        pageResult.setTotal(airplaneResult.getTotal());
        pageResult.setRecords(airplaneList);
        return pageResult;
    }

    /**
     * 查询飞机
     * 校验飞机是否存在
     * add方法在使用
     */
    private AirplaneEO getAirplane(String airplaneName){
        LambdaQueryWrapper<AirplaneEO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AirplaneEO::getAirplaneName, airplaneName);
        return airplaneMapper.selectOne(queryWrapper);
    }

    private void validateAirplaneIsExist(String airplaneName){
        AirplaneEO airplane = getAirplane(airplaneName);
        if (airplane != null){
            throw AIRPLANE_ALREADY_EXISTS.toException();
        }
    }
}
