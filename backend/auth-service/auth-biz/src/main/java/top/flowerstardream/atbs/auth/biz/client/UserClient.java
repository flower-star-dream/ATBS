package top.flowerstardream.atbs.auth.biz.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import top.flowerstardream.atbs.auth.ao.req.AuthUserREQ;
import top.flowerstardream.atbs.auth.ao.req.UserSynchronizeREQ;
import top.flowerstardream.base.ao.res.BaseVerificationRES;
import top.flowerstardream.base.result.Result;

import java.util.List;

/**
 * @Author: 花海
 * @Date: 2026/03/19/04:20
 * @Description: 用户服务客户端
 */
@FeignClient(name = "user-service", path = "api/internal/v1/user")
public interface UserClient {
    /**
     * 同步用户信息
     * @param userSynchronizeREQ
     */
    @GetMapping("/auth/synchronization")
    Result<Void> synchronizationUserInfo(@RequestParam UserSynchronizeREQ userSynchronizeREQ);

    /**
     * 注册微信小程序用户
     * @param userSynchronizeREQ
     */
    @PostMapping("/user/wxRegister")
    Result<Void> wxRegister(@RequestParam UserSynchronizeREQ userSynchronizeREQ);
}
