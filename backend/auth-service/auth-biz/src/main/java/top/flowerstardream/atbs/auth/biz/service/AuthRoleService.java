package top.flowerstardream.atbs.auth.biz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.auth.entity.AuthRole;

import java.util.List;

/**
 * 角色服务接口
 */
public interface AuthRoleService extends IService<AuthRole> {

    /**
     * 根据角色编码查询角色
     */
    AuthRole getByRoleCode(String roleCode);

    /**
     * 查询用户的所有角色
     */
    List<AuthRole> getRolesByUserId(Long userId);
}
