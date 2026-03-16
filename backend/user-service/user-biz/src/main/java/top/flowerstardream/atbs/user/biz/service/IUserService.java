package top.flowerstardream.atbs.user.biz.service;


import top.flowerstardream.atbs.user.ao.req.UserInfoREQ;
import top.flowerstardream.atbs.user.ao.req.UserPageQueryREQ;
import top.flowerstardream.atbs.user.bo.eo.UserEO;
import top.flowerstardream.base.ao.res.BaseStatusRES;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.service.IBaseService;
import top.flowerstardream.base.state.BaseStatus;

import java.util.List;

/**
 * 用户服务接口
 *
 * @author 花海
 * @date 2025-11-15
 */
public interface IUserService extends IBaseService<UserEO> {

    /**
     * 获取当前登录用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    List<UserEO> getUserInfo(List<Long> userId);

    /**
     * 更新用户信息
     *
     * @param userInfoREQ 用户信息请求
     */
    void updateUserInfo(UserInfoREQ userInfoREQ);

    /**
     * 分页查询用户列表（后管端接口使用）
     *
     * @param queryREQ 查询条件
     * @return 用户列表分页结果
     */
    PageResult<UserEO> list(UserPageQueryREQ queryREQ);

    /**
     * 获取用户状态列表
     *
     * @return 用户状态列表
     */
    List<BaseStatusRES<BaseStatus>> getStatus();

    /**
     * 根据用户名获取用户ID列表
     *
     * @param name 用户名
     * @return 用户ID列表
     */
    List<Long> getUserIdsByName(String name);
}
