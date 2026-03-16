package top.flowerstardream.atbs.user.biz.state;

import org.springframework.stereotype.Component;
import top.flowerstardream.atbs.user.biz.mapper.EmployeeMapper;
import top.flowerstardream.atbs.user.bo.eo.EmployeeEO;
import top.flowerstardream.base.annotation.StateRouter;
import top.flowerstardream.base.state.BaseEvent;
import top.flowerstardream.base.state.BaseRouter;
import top.flowerstardream.base.state.BaseStatus;
import top.flowerstardream.base.state.IStateRouter;

/**
 * @Author: 花海
 * @Date: 2026/03/08/21:50
 * @Description: 员工状态路由
 */
@Component
@StateRouter
public class EmployeeRouter extends BaseRouter<EmployeeMapper, EmployeeEO> implements IStateRouter<BaseStatus, BaseEvent, EmployeeEO> {
}
