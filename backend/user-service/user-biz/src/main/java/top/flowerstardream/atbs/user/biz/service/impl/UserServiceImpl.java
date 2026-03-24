package top.flowerstardream.atbs.user.biz.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import top.flowerstardream.atbs.user.ao.req.UserInfoREQ;
import top.flowerstardream.atbs.user.ao.req.UserPageQueryREQ;
import top.flowerstardream.atbs.user.ao.req.UserSynchronizeREQ;
import top.flowerstardream.atbs.user.biz.client.AuthClient;
import top.flowerstardream.atbs.user.biz.mapper.EmployeeMapper;
import top.flowerstardream.atbs.user.biz.mapper.UserMapper;
import top.flowerstardream.atbs.user.biz.service.IUserService;
import top.flowerstardream.atbs.user.bo.eo.EmployeeEO;
import top.flowerstardream.atbs.user.bo.eo.UserEO;
import top.flowerstardream.base.ao.res.BaseStatusRES;
import top.flowerstardream.base.properties.WeChatProperties;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.service.Impl.BaseServiceImpl;
import top.flowerstardream.base.state.BaseStatus;
import top.flowerstardream.base.utils.HttpClientUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static top.flowerstardream.atbs.user.common.DefaultParams.Avatar;
import static top.flowerstardream.atbs.user.common.UserRedisPrefixConstant.*;
import static top.flowerstardream.base.exception.ExceptionEnum.*;

/**
 * 用户服务实现类
 *
 * @author 花海
 * @date 2025-11-15
 */
@Slf4j
@Service
public class UserServiceImpl extends BaseServiceImpl<UserMapper, UserEO> implements IUserService {

    @Resource
    private AuthClient authClient;

    @Resource
    private UserMapper userMapper;

    @Resource
    @Lazy
    private IUserService self;

    @Override
    @Cacheable(cacheNames = USER_INFO_CACHE_NAME, key = "#ids", unless = "#ids == null")
    public List<UserEO> getUserInfo(List<Long> ids) {
        return self.listByIds(ids);
    }

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void updateUserInfo(UserInfoREQ userInfoREQ) {
        UserEO user = new UserEO();
        BeanUtils.copyProperties(userInfoREQ, user);
        if (!self.updateById(user)) {
            log.error("更新当前登录用户信息失败：{}", user);
            throw MODIFICATION_FAILED.toException();
        }
        toSynchronize(user, null);
    }

    @Override
    public PageResult<UserEO> list(UserPageQueryREQ queryREQ) {
        // 构建分页对象
        Page<UserEO> page = new Page<>(queryREQ.getPage(), queryREQ.getPageSize());
        
        // 构建查询条件
        LambdaQueryWrapper<UserEO> queryWrapper = new LambdaQueryWrapper<>();
        
        // 按用户名模糊查询
        if (StrUtil.isNotBlank(queryREQ.getUsername())) {
            queryWrapper.like(UserEO::getUsername, queryREQ.getUsername());
        }
        
        // 按手机号查询
        if (StrUtil.isNotBlank(queryREQ.getPhone())) {
            queryWrapper.eq(UserEO::getPhone, queryREQ.getPhone());
        }
        
        // 执行查询
        Page<UserEO> resultPage = userMapper.selectPage(page, queryWrapper);
        
        // 封装分页结果
        PageResult<UserEO> pageResult = new PageResult<>();
        pageResult.setTotal(resultPage.getTotal());
        pageResult.setRecords(resultPage.getRecords());
        
        return pageResult;
    }

    /**
     * 同步用户账号
     * @param userSynchronizeREQ
     */
    @Override
    public void synchronize(UserSynchronizeREQ userSynchronizeREQ) {
        log.info("同步用户账号: {}", userSynchronizeREQ);
        UserEO userEO = self.getById(userSynchronizeREQ.getId());
        if (userEO == null) {
            log.warn("同步用户账号失败，用户账号不存在");
            return;
        }
        if (StrUtil.isNotBlank(userSynchronizeREQ.getUsername())) {
            userEO.setUsername(userSynchronizeREQ.getUsername());
        }
        if (StrUtil.isNotBlank(userSynchronizeREQ.getPhone())) {
            userEO.setPhone(userSynchronizeREQ.getPhone());
        }
        if (StrUtil.isNotBlank(userSynchronizeREQ.getEmail())) {
            userEO.setEmail(userSynchronizeREQ.getEmail());
        }
        if (userSynchronizeREQ.getStatus() != null) {
            userEO.setStatus(userSynchronizeREQ.getStatus());
        }
        if (!self.updateById(userEO)) {
            log.error("同步用户账号失败：{}", userEO);
            throw MODIFICATION_FAILED.toException();
        }
    }

    /**
     * 注册微信小程序用户
     *
     * @param userSynchronizeREQ
     */
    @Override
    public void wxRegister(UserSynchronizeREQ userSynchronizeREQ) {
        UserEO user = new UserEO();
        BeanUtils.copyProperties(userSynchronizeREQ, user);
        user.setAvatar(Avatar);
        if (!self.save(user)) {
            log.error("新增员工账号失败：{}", user);
            throw INSERTION_FAILED.toException();
        }
    }

    /**
     * 根据用户名获取用户ID列表
     *
     * @param name 用户名
     * @return 用户ID列表
     */
    @Override
    public List<Long> getUserIdsByName(String name) {
        if (name == null || name.isEmpty()) {
            return Collections.emptyList();
        }
        return userMapper.selectList(new LambdaQueryWrapper<UserEO>()
            .like(UserEO::getUsername, name))
            .stream()
            .map(UserEO::getId)
            .toList();
    }

    private Long toSynchronize(UserEO userEO, String password) {
        UserSynchronizeREQ userSynchronizeREQ = new UserSynchronizeREQ();
        userSynchronizeREQ.setId(userEO.getId());
        userSynchronizeREQ.setUsername(userEO.getUsername());
        userSynchronizeREQ.setPhone(userEO.getPhone());
        return authClient.synchronizationUserInfo(userSynchronizeREQ).getData();
    }
}
