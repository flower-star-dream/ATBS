package top.flowerstardream.atbs.auth.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import top.flowerstardream.atbs.auth.biz.mapper.AuthUserSocialMapper;
import top.flowerstardream.atbs.auth.biz.service.IUserSocialService;
import top.flowerstardream.atbs.auth.bo.eo.UserSocialEO;

/**
 * @Author: 花海
 * @Date: 2026/03/19/17:40
 * @Description: 第三方绑定服务实现类
 */
@Service
public class UserSocialServiceImpl extends ServiceImpl<AuthUserSocialMapper, UserSocialEO> implements IUserSocialService {

    @Resource
    private AuthUserSocialMapper userSocialMapper;

    @Resource
    @Lazy
    private IUserSocialService self;

    /**
     * 根据openId查询用户第三方绑定信息
     *
     * @param openId
     * @return
     */
    @Override
    public UserSocialEO getUserSocialByOpenId(String openId) {
        LambdaQueryWrapper<UserSocialEO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSocialEO::getOpenid, openId);
        return self.getOne(wrapper);
    }
}
