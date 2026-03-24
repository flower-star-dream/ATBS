package top.flowerstardream.atbs.auth.biz.state;

import org.springframework.stereotype.Component;
import top.flowerstardream.atbs.auth.biz.mapper.AuthUserMapper;
import top.flowerstardream.atbs.auth.bo.eo.AuthUserEO;
import top.flowerstardream.base.annotation.StateRouter;
import top.flowerstardream.base.state.BaseEvent;
import top.flowerstardream.base.state.BaseRouter;
import top.flowerstardream.base.state.BaseStatus;
import top.flowerstardream.base.state.IStateRouter;

/**
 * @Author: 花海
 * @Date: 2026/03/17/16:43
 * @Description: 全局用户状态路由
 */
@Component
@StateRouter
public class AuthUserRouter extends BaseRouter<AuthUserMapper, AuthUserEO> implements IStateRouter<BaseStatus, BaseEvent, AuthUserEO> {
}
