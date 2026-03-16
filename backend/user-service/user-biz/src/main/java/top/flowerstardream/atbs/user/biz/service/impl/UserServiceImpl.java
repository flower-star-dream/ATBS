package top.flowerstardream.atbs.user.biz.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import top.flowerstardream.atbs.user.ao.req.UserInfoREQ;
import top.flowerstardream.atbs.user.ao.req.UserPageQueryREQ;
import top.flowerstardream.atbs.user.biz.mapper.UserMapper;
import top.flowerstardream.atbs.user.biz.service.IUserService;
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

    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Resource
    private UserMapper userMapper;

    @Resource
    private WeChatProperties weChatProperties;

    @Resource
    @Lazy
    private IUserService self;

    @Override
    @Cacheable(cacheNames = USER_INFO_CACHE_NAME, key = "#ids", unless = "#ids == null")
    public List<UserEO> getUserInfo(List<Long> ids) {
        return self.listByIds(ids);
    }

    @Override
    public void updateUserInfo(UserInfoREQ userInfoREQ) {
        UserEO user = new UserEO();
        BeanUtils.copyProperties(userInfoREQ, user);
        boolean update = updateById(user);
        if (!update) {
            log.error("更新当前登录用户信息失败：{}", user);
            throw MODIFICATION_FAILED.toException();
        }
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
     * 调取微信接口服务，获取微信用户的openid
     * @param code
     * @return
     */
    private String getOpenId(String code) {
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, map);

        JSONObject jsonObject = JSON.parseObject(json);
        return jsonObject.getString("openid");
    }

    @Override
    public List<BaseStatusRES<BaseStatus>> getStatus() {
        // 使用LambdaQueryWrapper进行分组统计
        List<Map<String, Object>> statusCounts = userMapper.count();

        // 将统计结果转换为BaseStatusRES列表
        return statusCounts.stream()
            .map(map -> {
                BaseStatusRES<BaseStatus> statusRES = new BaseStatusRES<>();
                statusRES.setStatus((BaseStatus) map.get("state"));
                statusRES.setCount((Integer) map.get("count"));
                statusRES.setDescription(statusRES.getStatus().getName());
                return statusRES;
            })
            .collect(Collectors.toList());
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
}
