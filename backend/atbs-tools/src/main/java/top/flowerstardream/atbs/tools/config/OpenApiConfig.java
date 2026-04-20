package top.flowerstardream.atbs.tools.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 配置类
 *
 * @Author: 花海
 * @Date: 2025/10/30
 * @Description: SpringDoc OpenAPI 公共配置
 */
@Configuration
public class OpenApiConfig {

    /**
     * 配置OpenAPI信息
     */
    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("飞机订票系统API")
                        .version("1.0.0")
                        .description("飞机订票系统文档")
                        .contact(new Contact()
                                .name("花海")
                                .email("flowerstardream@top")
                                .url("https://flower-star-dream.top"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                .addServersItem(new Server().url("/").description("本地服务器"));
    }

    /**
     * 默认API分组 - 包含所有接口
     */
    @Bean
    public GroupedOpenApi defaultApi() {
        return GroupedOpenApi.builder()
                .group("default")
                .pathsToMatch("/**")
                .build();
    }
}