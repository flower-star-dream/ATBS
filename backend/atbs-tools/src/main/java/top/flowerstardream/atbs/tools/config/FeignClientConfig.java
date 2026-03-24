package top.flowerstardream.atbs.tools.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: 花海
 * @Date: 2026/03/21/23:12
 * @Description: FeignClient配置类
 */
@Configuration
@EnableFeignClients(basePackages = "top.flowerstardream.atbs.tools.client")
public class FeignClientConfig {
}
