package top.flowerstardream.atbs.airplane;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 班次服务启动类
 *
 * @author flowerstardream
 * @date 2025-01-25
 */
@SpringBootApplication(scanBasePackages = {
        "top.flowerstardream.atbs.airplane",
        "top.flowerstardream.atbs.tools"
})
@EnableDiscoveryClient //开启服务注册与发现
@EnableFeignClients
@EnableTransactionManagement //开启注解方式的事务管理
@EnableScheduling //开启注解方式的定时任务
@EnableCaching //开启注解方式的缓存管理
@Slf4j
@MapperScan("top.flowerstardream.atbs.airplane.biz.mapper")
public class AirplaneApplication {

    public static void main(String[] args) {
        SpringApplication.run(AirplaneApplication.class, args);
        log.info("Airplane server started");
    }
}