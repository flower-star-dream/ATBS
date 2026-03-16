package top.flowerstardream.atbs.tools.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import top.flowerstardream.base.result.Result;

import java.util.List;
import java.util.Map;

/**
 * @author: 花海
 * @date: 2026/03/10 23:01
 * @description: 用户服务客户端接口
 */
@FeignClient(name = "user-service")
@Component
public interface UserClient {
    
    @PostMapping("/api/user/batchNames")
    Result<Map<Long, String>> batchGetNames(@RequestBody List<Long> userIds);
}