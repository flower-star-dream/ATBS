package top.flowerstardream.atbs.auth.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.flowerstardream.atbs.auth.bo.eo.PermissionEO;

import javax.security.auth.AuthPermission;
import java.util.List;

/**
 * 权限Mapper
 */
@Mapper
public interface AuthPermissionMapper extends BaseMapper<PermissionEO> {

}
