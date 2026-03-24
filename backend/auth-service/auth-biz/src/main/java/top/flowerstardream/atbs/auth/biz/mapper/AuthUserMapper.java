package top.flowerstardream.atbs.auth.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.flowerstardream.atbs.auth.bo.eo.AuthUserEO;
import top.flowerstardream.base.mapper.BaseMapperX;

/**
 * 用户Mapper
 */
@Mapper
public interface AuthUserMapper extends BaseMapperX<AuthUserEO> {

}
