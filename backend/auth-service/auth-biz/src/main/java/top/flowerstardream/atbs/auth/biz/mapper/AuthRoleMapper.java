package top.flowerstardream.atbs.auth.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.flowerstardream.atbs.auth.bo.eo.RoleEO;

import java.util.List;

/**
 * 角色Mapper
 */
@Mapper
public interface AuthRoleMapper extends BaseMapper<RoleEO> {

}
