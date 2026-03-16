package top.flowerstardream.atbs.gateway.client;

import reactivefeign.spring.config.ReactiveFeignClient;

/**
 * @Author: 花海
 * @Date: 2026/03/13/17:22
 * @Description: 鉴权服务客户端
 */
@ReactiveFeignClient(name = "auth-service", path = "/auth")
public interface AuthClient {

    void auth(String token, Integer clientType, Long userId, String username);
}
