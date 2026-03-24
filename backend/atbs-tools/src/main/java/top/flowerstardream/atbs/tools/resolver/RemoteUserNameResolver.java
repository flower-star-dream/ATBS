package top.flowerstardream.atbs.tools.resolver;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;
import top.flowerstardream.atbs.tools.client.AuthClient;
import top.flowerstardream.atbs.tools.interfaces.IUserResolveService;
import top.flowerstardream.base.handler.MyMetaObjectHandler;
import top.flowerstardream.base.resolver.UserNameResolver;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 远程用户名解析器
 * @author: 花海
 * @date: 2026/03/10 23:01
 * @description: 远程用户名解析器
 */
@Slf4j
@Component
@ConditionalOnClass({MybatisPlusInterceptor.class, MyMetaObjectHandler.class})
@RequiredArgsConstructor
public class RemoteUserNameResolver implements UserNameResolver {

    private final IUserResolveService iUserResolveService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    private static final String KEY_PREFIX = "sys:user:name:";
    private static final Duration TTL = Duration.ofHours(24);

    @Override
    public Map<Long, String> resolve(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<Long, String> result = new HashMap<>(userIds.size());
        Set<Long> missingIds = new HashSet<>();
        
        // 1. 查本地Redis
        List<String> keys = userIds.stream()
            .map(id -> KEY_PREFIX + id)
            .collect(Collectors.toList());
        
        List<String> cached = stringRedisTemplate.opsForValue().multiGet(keys);
        int idx = 0;
        for (Long id : userIds) {
            String name = null;
            if (cached != null) {
                name = cached.get(idx++);
            }
            if (name != null) {
                result.put(id, name);
            } else {
                missingIds.add(id);
            }
        }
        
        // 2. 缺失的远程调用用户服务
        if (!missingIds.isEmpty()) {
            Map<Long, String> fromRemote = iUserResolveService.batchGetNames(new ArrayList<>(missingIds)).getData();
            result.putAll(fromRemote);
            
            // 3. 异步写入本地Redis
            asyncWriteToRedis(fromRemote);
        }
        
        return result;
    }
    
    private void asyncWriteToRedis(Map<Long, String> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    data.forEach((id, name) -> {
                        byte[] key = (KEY_PREFIX + id).getBytes(StandardCharsets.UTF_8);
                        byte[] val = name.getBytes(StandardCharsets.UTF_8);
                        connection.stringCommands().set(key, val, Expiration.from(TTL), RedisStringCommands.SetOption.UPSERT);
                    });
                    return null;
                });
            } catch (Exception e) {
                // 记录日志，不影响主流程
                log.warn("异步写入Redis失败", e);
            }
        });
    }
}