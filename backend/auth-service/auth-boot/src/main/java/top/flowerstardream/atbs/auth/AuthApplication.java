package top.flowerstardream.atbs.auth;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import top.flowerstardream.atbs.tools.config.FeignClientConfig;

/**
 * @author 花海
 * @date 2026/01/23 14:04
 * @description 鉴权服务启动类
 */
@SpringBootApplication(scanBasePackages = {
        "top.flowerstardream.atbs.auth",
        "top.flowerstardream.atbs.tools"
})
@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = FeignClientConfig.class))
@EnableDiscoveryClient //开启服务注册与发现
@EnableFeignClients
@EnableTransactionManagement //开启注解方式的事务管理
@EnableScheduling //开启注解方式的定时任务
@EnableCaching //开启注解方式的缓存管理
@Slf4j
@MapperScan("top.flowerstardream.atbs.auth.biz.mapper")
public class AuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthApplication.class, args);
		log.info("Authorization server started");
	}

}
