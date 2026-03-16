package top.flowerstardream.atbs.user.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.flowerstardream.atbs.user.bo.eo.PassengerEO;
import top.flowerstardream.base.mapper.BaseMapperX;

import java.util.List;
import java.util.Map;

/**
 * @Author: 花海
 * @Date: 2025/11/10/22:38
 * @Description: 乘客Mapper
 */
@Mapper
public interface PassengerMapper extends BaseMapperX<PassengerEO> {
}
