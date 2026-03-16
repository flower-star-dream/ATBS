package top.flowerstardream.atbs.auth.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.flowerstardream.atbs.auth.bo.eo.RolePermissionEO;

/**
 * 角色权限关联Mapper
 */
@Mapper
public interface AuthRolePermissionMapper extends BaseMapper<RolePermissionEO> {
}
