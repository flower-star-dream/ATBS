package top.flowerstardream.atbs.user.biz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.flowerstardream.atbs.user.ao.req.*;
import top.flowerstardream.atbs.user.bo.eo.EmployeeEO;
import top.flowerstardream.base.ao.res.BaseStatusRES;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.service.IBaseService;
import top.flowerstardream.base.state.BaseStatus;

import java.util.List;

/**
 * @Author: 花海
 * @Date: 2025/10/26/21:56
 * @Description: 员工服务
 */
public interface IEmployeeService extends IBaseService<EmployeeEO> {

    /**
     * 获取当前登录用户信息
     * @param id
     * @return 当前登录用户信息
     */
    EmployeeEO getInfo(Long id);

    /**
     * 更新当前登录用户信息
     * @param employeeInfoREQ
     */
    void updateInfo(EmployeeInfoREQ employeeInfoREQ);

    /**
     * 获取员工列表
     * @param employeePageQueryREQ
     * @return
     */
    PageResult<EmployeeEO> list(EmployeePageQueryREQ employeePageQueryREQ);

    /**
     * 批量删除员工
     * @param ids
     */
    void delete(List<Long> ids);

    /**
     * 新增员工账号
     * @param employeeREQ
     */
    void add(EmployeeREQ employeeREQ);

    /**
     * 修改员工账号
     * @param employeeREQ
     */
    void update(EmployeeREQ employeeREQ);

    /**
     * 获取员工状态
     * @return
     */
    List<BaseStatusRES<BaseStatus>> getStatus();
}
