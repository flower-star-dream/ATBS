package top.flowerstardream.atbs.user.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.flowerstardream.atbs.user.bo.eo.UserPassengerEO;

/**
 * @Author: 花海
 * @Date: 2025/11/11
 * @Description: 用户乘客Mapper
 */
@Mapper
public interface UserPassengerMapper extends BaseMapper<UserPassengerEO> {
    
}