package top.flowerstardream.atbs.auth.biz.service;

import top.flowerstardream.atbs.auth.ao.req.*;
import top.flowerstardream.atbs.auth.ao.res.WxLoginTokenRES;
import top.flowerstardream.atbs.auth.bo.eo.AuthUserEO;
import top.flowerstardream.atbs.tools.interfaces.IUserResolveService;
import top.flowerstardream.base.ao.res.BaseStatusRES;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.result.Result;
import top.flowerstardream.base.service.IBaseService;

import java.util.List;
import java.util.Map;

/**
 * 用户服务接口
 * @author 花海
 * @date 2026/03/18/23:57
 * @description 用户服务接口
 */
public interface IAuthUserService extends IBaseService<AuthUserEO>, IUserResolveService {
    /**
     * 微信登录
     * @param loginREQ 微信登录请求
     * @return 登录响应
     */
    WxLoginTokenRES wechatLogin(WechatLoginREQ loginREQ);

    /**
     * 更新当前用户信息
     * @param userInfoREQ
     */
    void updateInfo(UserInfoREQ userInfoREQ);

    /**
     * 获取统一用户列表
     * @param userPageQueryREQ
     * @return
     */
    PageResult<AuthUserEO> list(UserPageQueryREQ userPageQueryREQ);

    /**
     * 新增用户账号
     * @param authUserREQ
     */
    void add(AuthUserREQ authUserREQ);

    /**
     * 修改用户账号
     * @param authUserREQ
     */
    void update(AuthUserREQ authUserREQ);

    /**
     * 重置密码
     * @param resetPwdREQ
     */
    void resetPassword(ResetPwdREQ resetPwdREQ);

    /**
     * 同步用户信息
     * @param userSynchronizeREQ
     */
    Long synchronize(UserSynchronizeREQ userSynchronizeREQ);

    /**
     * 批量获取用户名称
     * @param userIds
     * @return
     */
    @Override
    Result<Map<Long, String>> batchGetNames(List<Long> userIds);
}
