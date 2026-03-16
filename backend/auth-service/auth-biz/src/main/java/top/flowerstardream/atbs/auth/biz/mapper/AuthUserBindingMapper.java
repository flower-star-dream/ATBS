package top.flowerstardream.atbs.auth.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.flowerstardream.atbs.auth.bo.eo.UserSocialEO;

/**
 * 用户绑定Mapper
 */
@Mapper
public interface AuthUserBindingMapper extends BaseMapper<UserSocialEO> {

}
