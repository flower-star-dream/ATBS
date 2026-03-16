package top.flowerstardream.atbs.auth.biz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.auth.entity.AuthPermission;

import java.util.List;

/**
 * 权限服务接口
 */
public interface AuthPermissionService extends IService<AuthPermission> {

    /**
     * 根据权限编码查询权限
     */
    AuthPermission getByPermCode(String permCode);

    /**
     * 查询用户的所有权限
     */
    List<AuthPermission> getPermissionsByUserId(Long userId);

    /**
     * 查询角色的所有权限
     */
    List<AuthPermission> getPermissionsByRoleId(Long roleId);
}
