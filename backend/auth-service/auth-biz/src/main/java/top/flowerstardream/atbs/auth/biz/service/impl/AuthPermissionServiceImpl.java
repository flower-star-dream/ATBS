package top.flowerstardream.atbs.auth.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.flowerstardream.atbs.auth.biz.mapper.AuthPermissionMapper;
import top.flowerstardream.atbs.auth.biz.mapper.AuthRolePermissionMapper;
import top.flowerstardream.atbs.auth.biz.mapper.AuthUserRoleMapper;
import top.flowerstardream.atbs.auth.biz.service.IAuthPermissionService;
import top.flowerstardream.atbs.auth.bo.eo.PermissionEO;
import top.flowerstardream.atbs.auth.bo.eo.RolePermissionEO;
import top.flowerstardream.atbs.auth.bo.eo.UserRoleEO;

import java.util.List;

/**
 * @Author: 花海
 * @Date: 2026/03/19/17:04
 * @Description: 权限服务实现类
 */
@Service
public class AuthPermissionServiceImpl extends ServiceImpl<AuthPermissionMapper, PermissionEO> implements IAuthPermissionService {

    @Resource
    private AuthPermissionMapper authPermissionMapper;

    @Resource
    private AuthUserRoleMapper authUserRoleMapper;

    @Resource
    private AuthRolePermissionMapper authRolePermissionMapper;

    @Resource
    @Lazy
    private IAuthPermissionService self;
    /**
     * 根据权限编码查询权限
     *
     * @param permCode
     */
    @Override
    public PermissionEO getByPermCode(String permCode) {
        LambdaQueryWrapper<PermissionEO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PermissionEO::getPermCode, permCode);
        return authPermissionMapper.selectOne(queryWrapper);
    }

    /**
     * 查询用户的所有权限
     *
     * @param userId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PermissionEO> getPermissionsByUserId(Long userId) {
        LambdaQueryWrapper<UserRoleEO> userRoleWrapper = new LambdaQueryWrapper<>();
        userRoleWrapper.eq(UserRoleEO::getUserId, userId);
        List<UserRoleEO> userRoles = authUserRoleMapper.selectList(userRoleWrapper);
        LambdaQueryWrapper<RolePermissionEO> rolePermissionWrapper = new LambdaQueryWrapper<>();
        rolePermissionWrapper.in(RolePermissionEO::getRoleId, userRoles.stream().map(UserRoleEO::getRoleId).toList());
        List<RolePermissionEO> rolePermissions = authRolePermissionMapper.selectList(rolePermissionWrapper);
        LambdaQueryWrapper<PermissionEO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(PermissionEO::getId, rolePermissions.stream().map(RolePermissionEO::getPermissionId).toList());
        return authPermissionMapper.selectList(queryWrapper);
    }

    /**
     * 查询角色的所有权限
     *
     * @param roleId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PermissionEO> getPermissionsByRoleId(Long roleId) {
        LambdaQueryWrapper<RolePermissionEO> rolePermissionWrapper = new LambdaQueryWrapper<>();
        rolePermissionWrapper.eq(RolePermissionEO::getRoleId, roleId);
        List<RolePermissionEO> rolePermissions = authRolePermissionMapper.selectList(rolePermissionWrapper);
        LambdaQueryWrapper<PermissionEO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(PermissionEO::getId, rolePermissions.stream().map(RolePermissionEO::getPermissionId).toList());
        return authPermissionMapper.selectList(queryWrapper);
    }
}
