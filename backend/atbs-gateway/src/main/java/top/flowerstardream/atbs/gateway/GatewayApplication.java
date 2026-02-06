package top.flowerstardream.atbs.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @author 花海
 * @date 2026/01/23 14:04
 * @description 网关启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
@Slf4j
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
		log.info("gateway server started");
	}

}
