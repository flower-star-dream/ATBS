package top.flowerstardream.atbs.auth.biz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.flowerstardream.atbs.auth.bo.eo.RoleEO;

import java.util.List;

/**
 * 角色服务接口
 */
public interface IAuthRoleService extends IService<RoleEO> {

    /**
     * 根据角色编码查询角色
     */
    RoleEO getByRoleCode(String roleCode);

    /**
     * 查询用户的所有角色
     */
    List<RoleEO> getRolesByUserId(Long userId);
}
