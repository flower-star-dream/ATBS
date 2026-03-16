package top.flowerstardream.atbs.airplane.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import top.flowerstardream.atbs.airplane.bo.eo.SeatReservationEO;

import java.util.List;
import java.util.Map;

@Mapper
public interface SeatReservationMapper extends BaseMapper<SeatReservationEO> {

    void insertBatchSomeColumn(List<SeatReservationEO> seatReservations);

    @Select("select status, count(*) as count from atbs_seat_reservation group by status")
    List<Map<String, Object>> count();
}
