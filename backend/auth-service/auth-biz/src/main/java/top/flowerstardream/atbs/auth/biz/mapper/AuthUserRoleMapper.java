package top.flowerstardream.atbs.auth.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.flowerstardream.atbs.auth.bo.eo.UserRoleEO;

/**
 * 用户角色关联Mapper
 */
@Mapper
public interface AuthUserRoleMapper extends BaseMapper<UserRoleEO> {
}
