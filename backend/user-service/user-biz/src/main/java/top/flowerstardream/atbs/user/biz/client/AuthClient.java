package top.flowerstardream.atbs.user.biz.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import top.flowerstardream.atbs.tools.interfaces.IUserResolveService;
import top.flowerstardream.atbs.user.ao.req.UserSynchronizeREQ;
import top.flowerstardream.base.result.Result;

import java.util.List;
import java.util.Map;

/**
 * @Author: 花海
 * @Date: 2026/03/19/04:45
 * @Description: 认证服务客户端
 */
@FeignClient(name = "auth-service", path = "api/all/v1/auth")
public interface AuthClient extends IUserResolveService {
    /**
     * 同步用户信息
     * @param userSynchronizeREQ
     */
    @GetMapping("/user/synchronization")
    Result<Long> synchronizationUserInfo(@RequestParam UserSynchronizeREQ userSynchronizeREQ);

    /**
     * 批量获取用户名
     * @param userIds
     * @return
     */
    @Override
    @PostMapping("/batchNames")
    Result<Map<Long, String>> batchGetNames(@RequestBody List<Long> userIds);
}
