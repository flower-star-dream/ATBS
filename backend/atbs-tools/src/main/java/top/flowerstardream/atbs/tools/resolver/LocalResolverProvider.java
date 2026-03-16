package top.flowerstardream.atbs.tools.resolver;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.flowerstardream.base.resolver.UserNameResolver;
import top.flowerstardream.base.resolver.UserNameResolverProvider;

/**
 * 本地用户名解析器提供者类
 * @author: 花海
 * @date: 2026/03/10 23:01
 * @description: 本地用户名解析器提供者类
 */
@Component
public class LocalResolverProvider implements UserNameResolverProvider {
    
    @Resource
    private RemoteUserNameResolver resolver;
    
    @Override
    public UserNameResolver getResolver() {
        return resolver;
    }
}