package top.flowerstardream.atbs.tools.client;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import top.flowerstardream.atbs.tools.interfaces.IUserResolveService;
import top.flowerstardream.base.annotation.ConditionalOnSpel;
import top.flowerstardream.base.handler.MyMetaObjectHandler;
import top.flowerstardream.base.result.Result;

import java.util.List;
import java.util.Map;

/**
 * @author: 花海
 * @date: 2026/03/10 23:01
 * @description: 鉴权服务客户端接口
 */
@FeignClient(name = "auth-service", path = "api/all/v1/auth/user")
@ConditionalOnClass({MybatisPlusInterceptor.class, MyMetaObjectHandler.class})
@ConditionalOnSpel("'${spring.application.name:}' != 'auth-service'")
public interface AuthClient extends IUserResolveService {
    
    @Override
    @PostMapping("/batchNames")
    Result<Map<Long, String>> batchGetNames(@RequestBody List<Long> userIds);
}