package top.flowerstardream.atbs.airplane.biz.state;

import org.springframework.stereotype.Component;
import top.flowerstardream.atbs.airplane.biz.mapper.SeatReservationMapper;
import top.flowerstardream.atbs.airplane.bo.eo.SeatReservationEO;
import top.flowerstardream.base.annotation.StateRouter;
import top.flowerstardream.base.state.BaseEvent;
import top.flowerstardream.base.state.BaseRouter;
import top.flowerstardream.base.state.BaseStatus;
import top.flowerstardream.base.state.IStateRouter;

/**
 * @Author: 花海
 * @Date: 2026/03/08/21:50
 * @Description: 座位预定状态路由
 */
@Component
@StateRouter
public class SeatReservationRouter extends BaseRouter<SeatReservationMapper, SeatReservationEO> implements IStateRouter<BaseStatus, BaseEvent, SeatReservationEO> {
}
