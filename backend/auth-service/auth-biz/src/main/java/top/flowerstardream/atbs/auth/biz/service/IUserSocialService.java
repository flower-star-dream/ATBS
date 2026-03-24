package top.flowerstardream.atbs.auth.biz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.flowerstardream.atbs.auth.bo.eo.UserSocialEO;

/**
 * @Author: 花海
 * @Date: 2026/03/19/17:39
 * @Description: 第三方用户业务服务接口
 */
public interface IUserSocialService extends IService<UserSocialEO> {

    /**
     * 根据openId查询用户第三方绑定信息
     * @param openId
     * @return
     */
    UserSocialEO getUserSocialByOpenId(String openId);

}
