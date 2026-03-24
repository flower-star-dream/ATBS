package top.flowerstardream.atbs.tools.resolver;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import top.flowerstardream.atbs.tools.interfaces.IUserResolveService;
import top.flowerstardream.base.handler.MyMetaObjectHandler;
import top.flowerstardream.base.resolver.UserNameResolver;
import top.flowerstardream.base.resolver.UserNameResolverProvider;

/**
 * 本地用户名解析器提供者类
 * @author: 花海
 * @date: 2026/03/10 23:01
 * @description: 本地用户名解析器提供者类
 */
@Component
@ConditionalOnClass({MybatisPlusInterceptor.class, MyMetaObjectHandler.class})
@RequiredArgsConstructor
public class LocalResolverProvider implements UserNameResolverProvider {

    private final RemoteUserNameResolver resolver;
    
    @Override
    public UserNameResolver getResolver() {
        return resolver;
    }
}