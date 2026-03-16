package top.flowerstardream.atbs.user.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import top.flowerstardream.atbs.user.bo.eo.UserEO;
import top.flowerstardream.base.mapper.BaseMapperX;

import java.util.List;
import java.util.Map;

/**
 * @Author: 花海
 * @Date: 2025/11/01/01:10
 * @Description: 用户Mapper
 */
@Mapper
public interface UserMapper extends BaseMapperX<UserEO> {

    /**
     * 根据openid查询用户信息
     *
     * @param openid
     * @return
     */
    @Select("select * from atbs_user where openid = #{openid}")
    UserEO getByOpenId(String openid);

    @Select("select status, count(*) as count from atbs_user group by status")
    List<Map<String, Object>> count();
}
