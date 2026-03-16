package top.flowerstardream.atbs.user.biz.state;

import org.springframework.stereotype.Component;
import top.flowerstardream.atbs.user.biz.mapper.UserMapper;
import top.flowerstardream.atbs.user.bo.eo.UserEO;
import top.flowerstardream.base.annotation.StateRouter;
import top.flowerstardream.base.state.BaseEvent;
import top.flowerstardream.base.state.BaseRouter;
import top.flowerstardream.base.state.BaseStatus;
import top.flowerstardream.base.state.IStateRouter;

/**
 * @Author: 花海
 * @Date: 2026/03/08/21:50
 * @Description: 用户状态路由
 */
@Component
@StateRouter
public class UserRouter extends BaseRouter<UserMapper, UserEO> implements IStateRouter<BaseStatus, BaseEvent, UserEO> {
}
