package top.flowerstardream.atbs.user.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import top.flowerstardream.atbs.user.ao.req.*;
import top.flowerstardream.atbs.user.biz.client.AuthClient;
import top.flowerstardream.atbs.user.biz.mapper.EmployeeMapper;
import top.flowerstardream.atbs.user.biz.service.IEmployeeService;
import top.flowerstardream.atbs.user.bo.eo.EmployeeEO;
import top.flowerstardream.base.ao.res.BaseStatusRES;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.service.Impl.BaseServiceImpl;
import top.flowerstardream.base.state.BaseStatus;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static top.flowerstardream.atbs.user.common.DefaultParams.Avatar;
import static top.flowerstardream.atbs.user.common.UserExceptionEnum.*;
import static top.flowerstardream.atbs.user.common.UserRedisPrefixConstant.*;
import static top.flowerstardream.base.exception.BaseExceptionEnum.*;
import static top.flowerstardream.base.exception.ExceptionEnum.*;
import static top.flowerstardream.base.state.BaseStatus.*;


/**
 * @Author: 花海
 * @Date: 2025/10/26/21:57
 * @Description: 后管员工服务实现类
 */
@Slf4j
@Service
public class EmployeeServiceImpl extends BaseServiceImpl<EmployeeMapper, EmployeeEO> implements IEmployeeService {

    @Resource
    private AuthClient authClient;

    @Resource
    private EmployeeMapper employeeMapper;

    @Resource
    @Lazy
    private IEmployeeService self;

    @Override
    @Cacheable(cacheNames = EMPLOYEE_INFO_CACHE_NAME, key = "#id", unless = "#id == null")
    public EmployeeEO getInfo(Long id) {
        log.info("获取当前登录用户信息：{}", id);
        return self.getById(id);
    }

    /**
     * 更新当前登录用户信息
     *
     * @param employeeInfoREQ
     */
    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void updateInfo(EmployeeInfoREQ employeeInfoREQ) {
        EmployeeEO employee = new EmployeeEO();
        BeanUtils.copyProperties(employeeInfoREQ, employee);
        boolean update = self.updateById(employee);
        if (!update) {
            log.error("更新当前登录用户信息失败：{}", employee);
            throw MODIFICATION_FAILED.toException();
        }
        toSynchronize(employee, null);
    }

    /**
     * 获取员工列表
     * @param employeePageQueryREQ
     * @return
     */
    @Override
    public PageResult<EmployeeEO> list(EmployeePageQueryREQ employeePageQueryREQ) {
        // 参数校验
        if (employeePageQueryREQ == null) {
            throw PARAM_ERROR.toException();
        }

        // 设置默认值
        if (employeePageQueryREQ.getPage() <= 0) {
            employeePageQueryREQ.setPage(1);
        }
        if (employeePageQueryREQ.getPageSize() <= 0) {
            employeePageQueryREQ.setPageSize(10);
        }

        // 构建分页查询条件
        Page<EmployeeEO> page = new Page<>(employeePageQueryREQ.getPage(), employeePageQueryREQ.getPageSize());
        LambdaQueryWrapper<EmployeeEO> queryWrapper = Wrappers.lambdaQuery();

        // 添加查询条件
        if (StringUtils.isNotBlank(employeePageQueryREQ.getUsername())) {
            queryWrapper.like(EmployeeEO::getUsername, employeePageQueryREQ.getUsername());
        }
        if (StringUtils.isNotBlank(employeePageQueryREQ.getPhone())) {
            queryWrapper.like(EmployeeEO::getPhone, employeePageQueryREQ.getPhone());
        }

        // 执行分页查询
        IPage<EmployeeEO> employeePage = employeeMapper.selectPage(page, queryWrapper);

        // 封装返回结果
        PageResult<EmployeeEO> pageResult = new PageResult<>();
        pageResult.setTotal(employeePage.getTotal());
        pageResult.setRecords(employeePage.getRecords());

        // 返回结果
        return pageResult;
    }

    /**
     * 批量删除员工
     *
     * @param ids
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        ids.forEach(id -> {
            // 获取员工信息
            EmployeeEO employee = self.getById(id);
            if (employee == null) {
                return;
            }
            if (ENABLE.equals(employee.getStatus())) {
                throw USER_STATUS_ENABLE.toException();
            }
        });
        // 批量删除员工
        boolean delete = self.removeByIds(ids);
        if (!delete) {
            log.error("批量删除员工失败：{}", ids);
            throw DELETION_FAILED.toException();
        }
    }

    /**
     * 新增员工账号
     *
     * @param employeeREQ
     */
    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void add(EmployeeREQ employeeREQ) {
        validateEmployeeIsNotEmpty(employeeREQ.getUsername(), employeeREQ.getPhone());
        EmployeeEO employee = new EmployeeEO();
        BeanUtils.copyProperties(employeeREQ, employee);
        employee.setId(null);
        Long userId = toSynchronize(employee, employeeREQ.getPassword());
        employee.setId(userId);
        employee.setAvatar(Avatar);
        if (!self.save(employee)) {
            log.error("新增员工账号失败：{}", employee);
            throw INSERTION_FAILED.toException();
        }
    }

    /**
     * 修改员工账号
     *
     * @param employeeREQ
     */
    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void update(EmployeeREQ employeeREQ) {
        validateEmployeeIsEmpty(employeeREQ.getUsername(), employeeREQ.getPhone());
        log.info("更新当前登录用户id：{}", employeeREQ.getId());
        EmployeeEO employee = new EmployeeEO();
        BeanUtils.copyProperties(employeeREQ, employee);
        log.info("更新当前登录用户信息：{}", employee);
        boolean update = self.updateById(employee);
        if (!update) {
            log.error("更新员工账号失败：{}", employee);
            throw MODIFICATION_FAILED.toException();
        }
        toSynchronize(employee, employeeREQ.getPassword());
    }

    /**
     * 同步员工账号
     * @param userSynchronizeREQ
     */
    @Override
    public void synchronize(UserSynchronizeREQ userSynchronizeREQ) {
        log.info("同步员工账号: {}", userSynchronizeREQ);
        EmployeeEO employee = self.getById(userSynchronizeREQ.getId());
        if (employee == null) {
            log.warn("同步员工账号失败，员工账号不存在");
            return;
        }
        if (StrUtil.isNotBlank(userSynchronizeREQ.getUsername())) {
            employee.setUsername(userSynchronizeREQ.getUsername());
        }
        if (StrUtil.isNotBlank(userSynchronizeREQ.getPhone())) {
            employee.setPhone(userSynchronizeREQ.getPhone());
        }
        if (userSynchronizeREQ.getStatus() != null) {
            employee.setStatus(userSynchronizeREQ.getStatus());
        }
        if (StrUtil.isNotBlank(userSynchronizeREQ.getPermissionLevel())) {
            employee.setPermissionLevel(userSynchronizeREQ.getPermissionLevel());
        }
        if (!self.updateById(employee)) {
            log.error("同步员工账号失败：{}", employee);
            throw MODIFICATION_FAILED.toException();
        }
    }

    /**
     * 通过用户名或手机号查询员工账号
     * @param username
     * @param phone
     * @return
     */
    private EmployeeEO getEmployee(String username, String phone) {
        EmployeeEO employee = null;
        if(StringUtils.isNotBlank(username)){
            employee = getOne(Wrappers.<EmployeeEO>lambdaQuery().eq(EmployeeEO::getUsername, username));
        }
        if(StringUtils.isNotBlank(phone)){
            employee = getOne(Wrappers.<EmployeeEO>lambdaQuery().eq(EmployeeEO::getPhone, phone));
        }
        return employee;
    }

    /**
     * 验证员工账号是否为空
     * @param username
     * @param phone
     * @return
     */
    private EmployeeEO validateEmployeeIsEmpty(String username, String phone) {
        EmployeeEO employeeEO = getEmployee(username, phone);
        if (employeeEO != null) {
            return employeeEO;
        }
        throw USER_NOT_EXIST.toException();
    }

    /**
     * 验证员工账号是否非空
     * @param username
     * @param phone
     * @return
     */
    private void validateEmployeeIsNotEmpty(String username, String phone) {
        EmployeeEO employeeEO = getEmployee(username, phone);
        if (employeeEO == null) {
            return; // 用户不存在
        }
        throw USER_ALREADY_EXISTS.toException();
    }

    private Long toSynchronize(EmployeeEO employeeEO, String password) {
        UserSynchronizeREQ userSynchronizeREQ = new UserSynchronizeREQ();
        userSynchronizeREQ.setId(employeeEO.getId());
        userSynchronizeREQ.setUsername(employeeEO.getUsername());
        userSynchronizeREQ.setPassword(password);
        userSynchronizeREQ.setPhone(employeeEO.getPhone());
        userSynchronizeREQ.setPermissionLevel(employeeEO.getPermissionLevel());
        return authClient.synchronizationUserInfo(userSynchronizeREQ).getData();
    }
}