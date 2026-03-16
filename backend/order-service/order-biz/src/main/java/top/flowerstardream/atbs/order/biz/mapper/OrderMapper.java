package top.flowerstardream.atbs.order.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import top.flowerstardream.atbs.order.bo.eo.OrderEO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @Author: 花海
 * @Date: 2025/11/11
 * @Description: 订单Mapper接口
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEO> {

    @Select("select status, count(*) from atbs_order group by status")
    List<Map<String, Object>> count();
}