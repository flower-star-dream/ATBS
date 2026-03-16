package top.flowerstardream.atbs.order.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import top.flowerstardream.atbs.order.bo.eo.TicketEO;

import java.util.List;
import java.util.Map;

/**
 * @Author: 花海
 * @Date: 2024/11/11
 * @Description: 机票数据访问接口
 */
@Mapper
public interface TicketMapper extends BaseMapper<TicketEO> {

    @Select("select status, count(*) from atbs_ticket group by status")
    List<Map<String, Object>> count();
}