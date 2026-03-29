package top.flowerstardream.atbs.tools.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.flowerstardream.atbs.tools.interfaces.TtlContextInterceptor;

/**
 * @Author: 花海
 * @Date: 2025/11/07/13:33
 * @Description: web配置类
 */
@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private TtlContextInterceptor ttlContextInterceptor;

    /**
     * 添加ttl上下文拦截器
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("【web配置类】添加ttl上下文拦截器");
        registry.addInterceptor(ttlContextInterceptor)
                .addPathPatterns("/**");
        log.info("【web配置类】添加ttl上下文拦截器完成");
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        log.info("【web配置类】添加Long转String序列化");
        return builder -> builder
                .serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(Long.TYPE, ToStringSerializer.instance);
    }

}