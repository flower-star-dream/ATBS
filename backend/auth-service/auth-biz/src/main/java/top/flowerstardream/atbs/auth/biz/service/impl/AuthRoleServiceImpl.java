package top.flowerstardream.atbs.auth.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.flowerstardream.atbs.auth.biz.mapper.AuthRoleMapper;
import top.flowerstardream.atbs.auth.biz.mapper.AuthUserRoleMapper;
import top.flowerstardream.atbs.auth.biz.service.IAuthRoleService;
import top.flowerstardream.atbs.auth.bo.eo.RoleEO;
import top.flowerstardream.atbs.auth.bo.eo.UserRoleEO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author: 花海
 * @Date: 2026/03/19/17:15
 * @Description: 角色服务接口实现类
 */
@Service
public class AuthRoleServiceImpl extends ServiceImpl<AuthRoleMapper, RoleEO> implements IAuthRoleService {

    @Resource
    private AuthRoleMapper authRoleMapper;

    @Resource
    private AuthUserRoleMapper authUserRoleMapper;

    @Resource
    @Lazy
    private IAuthRoleService self;
    /**
     * 根据角色编码查询角色
     *
     * @param roleCode
     */
    @Override
    public RoleEO getByRoleCode(String roleCode) {
        LambdaQueryWrapper<RoleEO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RoleEO::getRoleCode, roleCode);
        return self.getOne(queryWrapper);
    }

    /**
     * 查询用户的所有角色
     *
     * @param userId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RoleEO> getRolesByUserId(Long userId) {
        LambdaQueryWrapper<UserRoleEO> userRoleWrapper = new LambdaQueryWrapper<>();
        userRoleWrapper.eq(UserRoleEO::getUserId, userId);
        List<UserRoleEO> userRoles = authUserRoleMapper.selectList(userRoleWrapper);
        if (userRoles == null || userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<RoleEO> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.in(RoleEO::getId, userRoles.stream().map(UserRoleEO::getRoleId).toList());
        return authRoleMapper.selectList(roleWrapper);
    }

    /**
     * 设置用户的角色
     *
     * @param userId
     * @param roleCodes
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setRolesByUserId(Long userId, List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<UserRoleEO> userRoleWrapper = new LambdaQueryWrapper<>();
        userRoleWrapper.eq(UserRoleEO::getUserId, userId);
        List<UserRoleEO> userRoles = authUserRoleMapper.selectList(userRoleWrapper);
        if (userRoles != null && !userRoles.isEmpty()) {
            authUserRoleMapper.delete(userRoleWrapper);
        }
        List<UserRoleEO> newUserRoles = new ArrayList<>();
        for (String roleCode : roleCodes) {
            RoleEO role = self.getByRoleCode(roleCode);
            if (role == null) {
                continue;
            }
            UserRoleEO userRole = new UserRoleEO();
            userRole.setUserId(userId);
            userRole.setRoleId(role.getId());
            newUserRoles.add(userRole);
        }
        authUserRoleMapper.insert(newUserRoles);
    }
}
