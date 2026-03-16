package top.flowerstardream.atbs.user.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.flowerstardream.atbs.user.ao.req.PassengerPageQueryREQ;
import top.flowerstardream.atbs.user.ao.req.PassengerREQ;
import top.flowerstardream.atbs.user.ao.res.PassengerRES;
import top.flowerstardream.atbs.user.biz.mapper.PassengerMapper;
import top.flowerstardream.atbs.user.biz.mapper.UserMapper;
import top.flowerstardream.atbs.user.biz.mapper.UserPassengerMapper;
import top.flowerstardream.atbs.user.biz.service.IPassengerService;
import top.flowerstardream.atbs.user.bo.dto.PassengerDTO;
import top.flowerstardream.atbs.user.bo.eo.PassengerEO;
import top.flowerstardream.atbs.user.bo.eo.UserEO;
import top.flowerstardream.atbs.user.bo.eo.UserPassengerEO;
import top.flowerstardream.base.result.PageResult;

import java.util.List;
import java.util.stream.Collectors;

import static top.flowerstardream.atbs.user.common.UserExceptionEnum.*;
import static top.flowerstardream.base.exception.BaseExceptionEnum.*;


/**
 * @Author: 花海
 * @Date: 2025-11-10
 * @Description: 乘客服务实现类
 */
@Service
@Slf4j
public class PassengerServiceImpl extends ServiceImpl<PassengerMapper, PassengerEO> implements IPassengerService {

    @Resource
    private PassengerMapper passengerMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserPassengerMapper userPassengerMapper;

    @Resource
    @Lazy
    private IPassengerService self;

    /**
     * 分页查询乘客列表
     *
     * @param queryREQ 查询条件
     * @return 乘客列表分页结果
     */
    @Override
    public PageResult<PassengerEO> pageQuery(PassengerPageQueryREQ queryREQ) {
        
        // 参数校验
        if (queryREQ == null) {
            throw  THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }

        // 设置默认值
        if (queryREQ.getPage() <= 0) {
            queryREQ.setPage(1);
        }
        if (queryREQ.getPageSize() <= 0) {
            queryREQ.setPageSize(10);
        }
        
        // 创建分页对象
        Page<PassengerEO> page = new Page<>(queryREQ.getPage(), queryREQ.getPageSize());
        LambdaQueryWrapper<PassengerEO> queryWrapper = Wrappers.lambdaQuery();
        
        // 真实姓名模糊查询
        if (StringUtils.isNotBlank(queryREQ.getRealName())) {
            queryWrapper.like(PassengerEO::getRealName, queryREQ.getRealName());
        }
        
        // 证件类型精确查询
        if (StringUtils.isNotBlank(queryREQ.getCardType())) {
            queryWrapper.eq(PassengerEO::getCardType, queryREQ.getCardType());
        }
        
        // 身份证号模糊查询
        if (StringUtils.isNotBlank(queryREQ.getIdCard())) {
            queryWrapper.like(PassengerEO::getIdCard, queryREQ.getIdCard());
        }
        
        // 执行分页查询
        IPage<PassengerEO> passengerPage = passengerMapper.selectPage(page, queryWrapper);

        // 封装返回结果
        PageResult<PassengerEO> pageResult = new PageResult<>();
        pageResult.setTotal(passengerPage.getTotal());
        pageResult.setRecords(passengerPage.getRecords());
        return pageResult;
    }

    /**
     * 根据ID查询乘客详情
     *
     * @param id 乘客ID
     * @return 乘客详情
     */
    @Override
    public PassengerEO query(Long id) {
        // 参数校验
        if (id == null || id < 0) {
            throw PARAM_ERROR.toException();
        }
        // 根据ID查询乘客
        return getById(id);
    }

    /**
     * 获取当前用户关联的乘客列表
     *
     * @param userId 用户ID
     * @return 乘客列表
     */
    @Override
    public List<PassengerRES> getUserPassengers(Long userId) {
        // 参数校验
        if (userId == null || userId < 0) {
            throw PARAM_ERROR.toException();
        }

        // 查询用户关联的乘客ID列表
        LambdaQueryWrapper<UserPassengerEO> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(UserPassengerEO::getUserId, userId);
        List<UserPassengerEO> userPassengerList = userPassengerMapper.selectList(wrapper);

        // 如果没有关联的乘客，返回空列表
        if (userPassengerList.isEmpty()) {
            return List.of();
        }

        // 提取乘客ID列表
        List<Long> passengerIds = userPassengerList.stream()
                .map(UserPassengerEO::getPassengerId)
                .toList();

        // 查询乘客详情
        List<PassengerEO> passengerList = passengerMapper.selectBatchIds(passengerIds);

        // 转换为DTO
        return passengerList.stream()
                .map(passenger -> {
                    PassengerRES dto = new PassengerRES();
                    BeanUtils.copyProperties(passenger, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 新增乘客
     *
     * @param userId 用户ID
     * @param passengerREQ 乘客信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPassenger(Long userId, PassengerREQ passengerREQ) {
        // 参数校验
        if (userId == null || userId <= 0) {
            throw PARAM_ERROR.toException();
        }
        if (passengerREQ == null) {
            throw THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }

        // 根据身份证号查询是否已存在乘客
        LambdaQueryWrapper<PassengerEO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(PassengerEO::getIdCard, passengerREQ.getIdCard());
        PassengerEO existingPassenger = passengerMapper.selectOne(queryWrapper);

        Long passengerId;

        if (existingPassenger != null) {
            // 如果已存在乘客，直接关联
            passengerId = existingPassenger.getId();
        } else {
            // 如果不存在，新增乘客
            PassengerEO passenger = new PassengerEO();
            BeanUtils.copyProperties(passengerREQ, passenger);
            passengerMapper.insert(passenger);
            passengerId = passenger.getId();
        }

        // 检查是否已关联
        LambdaQueryWrapper<UserPassengerEO> userPassengerWrapper = Wrappers.lambdaQuery();
        userPassengerWrapper.eq(UserPassengerEO::getUserId, userId)
                .eq(UserPassengerEO::getPassengerId, passengerId);
        UserPassengerEO existingUserPassenger = userPassengerMapper.selectOne(userPassengerWrapper);

        if (existingUserPassenger == null) {
            // 创建用户乘客关联
            UserPassengerEO userPassenger = UserPassengerEO.builder()
                    .userId(userId)
                    .passengerId(passengerId)
                    .build();
            userPassengerMapper.insert(userPassenger);
        }

        // 检查用户的passengerId是否为空，如果为空则更新
        UserEO user = userMapper.selectById(userId);
        if (user != null && user.getPassengerId() == 0) {
            user.setPassengerId(passengerId);
            userMapper.updateById(user);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultPassenger(Long userId, Long passengerId) {
        // 参数校验
        if (userId == null || userId <= 0) {
            throw PARAM_ERROR.toException();
        }
        if (passengerId == null || passengerId <= 0) {
            throw PARAM_ERROR.toException();
        }

        // 验证乘客是否存在
        PassengerEO passenger = passengerMapper.selectById(passengerId);
        if (passenger == null) {
            throw DATA_DOES_NOT_EXIST.toException();
        }

        // 验证乘客是否与当前用户关联
        LambdaQueryWrapper<UserPassengerEO> userPassengerWrapper = Wrappers.lambdaQuery();
        userPassengerWrapper.eq(UserPassengerEO::getUserId, userId)
                .eq(UserPassengerEO::getPassengerId, passengerId);
        UserPassengerEO userPassenger = userPassengerMapper.selectOne(userPassengerWrapper);
        if (userPassenger == null) {
            // 乘客未关联到当前用户
            throw THIRD_PARTY_DATA_DOES_NOT_EXIST.toException();
        }

        // 更新用户的默认乘客ID
        UserEO user = userMapper.selectById(userId);
        if (user == null) {
            throw USER_NOT_EXIST.toException();
        }
        user.setPassengerId(passengerId);
        userMapper.updateById(user);
    }

    /**
     * 根据乘客姓名获取乘客列表
     *
     * @param passengerName 乘客姓名
     * @return 乘客列表
     */
    @Override
    public List<Long> getPassengersByName(String passengerName) {
        if (StringUtils.isBlank(passengerName)) {
            return List.of();
        }
        LambdaQueryWrapper<PassengerEO> wrapper = Wrappers.lambdaQuery();
        wrapper.like(PassengerEO::getRealName, passengerName);
        return passengerMapper.selectList(wrapper).stream()
                .map(PassengerEO::getId)
                .collect(Collectors.toList());
    }

    /**
     * 根据乘客ID列表获取乘客信息列表
     *
     * @param passengerIds 乘客ID列表
     * @return 乘客信息列表
     */
    @Override
    public List<PassengerDTO> getPassengersByIds(List<Long> passengerIds) {
        if (CollUtil.isEmpty(passengerIds)) {
            return List.of();
        }
        return passengerMapper.selectBatchIds(passengerIds).stream()
                .map(passenger -> {
                    PassengerDTO dto = new PassengerDTO();
                    BeanUtils.copyProperties(passenger, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
