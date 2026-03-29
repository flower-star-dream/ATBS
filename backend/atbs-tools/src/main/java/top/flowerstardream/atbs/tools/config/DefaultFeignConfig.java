package top.flowerstardream.atbs.tools.config;

import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.flowerstardream.atbs.tools.interfaces.FeignInterceptor;
import top.flowerstardream.base.utils.ResultAwareErrorDecoder;

/**
 * @Author: 花海
 * @Date: 2025/11/07/15:21
 * @Description: 默认feign配置
 */
@Configuration
@Slf4j
public class DefaultFeignConfig {
    @Bean
    public FeignInterceptor traceFeignInterceptor() {
        return new FeignInterceptor();
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new ResultAwareErrorDecoder();
    }
}
