package top.flowerstardream.atbs.auth.biz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.flowerstardream.atbs.auth.bo.eo.PermissionEO;

import java.util.List;

/**
 * 权限服务接口
 */
public interface IAuthPermissionService extends IService<PermissionEO> {

    /**
     * 根据权限编码查询权限
     */
    PermissionEO getByPermCode(String permCode);

    /**
     * 查询用户的所有权限
     */
    List<PermissionEO> getPermissionsByUserId(Long userId);

    /**
     * 查询角色的所有权限
     */
    List<PermissionEO> getPermissionsByRoleId(Long roleId);
}
