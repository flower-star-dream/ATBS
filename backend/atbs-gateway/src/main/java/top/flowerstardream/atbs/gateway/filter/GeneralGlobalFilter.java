package top.flowerstardream.atbs.gateway.filter;

import cn.hutool.core.lang.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import top.flowerstardream.base.result.Result;
import top.flowerstardream.base.properties.JwtProperties;
import top.flowerstardream.base.properties.MyGatewayProperties;


/**
 * @Author: 花海
 * @Date: 2025/10/26/21:05
 * @Description: 全局过滤器
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class GeneralGlobalFilter implements GlobalFilter, Ordered {
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final MyGatewayProperties myGatewayProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = UUID.randomUUID().toString();
        log.info("【网关】生成 traceId={}", traceId);
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 白名单 & OPTIONS
        boolean isWhitelisted = WhiteListUtil.shouldSkip(path, myGatewayProperties.getWhiteList());
        boolean isOptions = "OPTIONS".equalsIgnoreCase(request.getMethod().name());
        log.info("【网关】白名单检查 - path={}, isWhitelisted={}, isOptions={}", path, isWhitelisted, isOptions);
        
        if (isWhitelisted || isOptions) {
            log.info("【网关】请求被白名单放行或为OPTIONS请求");
            return chain.filter(exchange);
        }
        
        log.info("【网关】请求未被白名单匹配，进入JWT校验流程");

        // 2. 统一 JWT 校验
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String bizSideHeader = request.getHeaders().getFirst(BIZ_SIDE);
        
        log.info("【网关】准备进行JWT校验 - authHeader={}, bizSideHeader={}", 
                 authHeader != null ? "存在" : "不存在", 
                 bizSideHeader);
        
        JwtValidator.ValidateResult vr = JwtValidator.validate(
                authHeader,
                bizSideHeader,
                jwtProperties,
                stringRedisTemplate);
        
        log.info("【网关】JWT校验完成 - valid={}, msg={}", vr.isValid(), vr.getMsg());

        if (!vr.isValid()) {
            log.warn("【网关】JWT校验失败，返回401 - path={}, msg={}", path, vr.getMsg());
            return ResponseWriter.write(exchange,
                    HttpStatus.UNAUTHORIZED,
                    Result.successResult(401, vr.getMsg()));
        }

        // 3. 角色鉴权
        String permission = vr.getPermissionLevel().toString();
        log.info("【网关】准备进行角色鉴权 - permission={}", permission);
        String error = PermissionMatcher.check(path, permission, myGatewayProperties.getAuthMatrix());
        log.info("【网关】角色鉴权完成 - error={}", error);
        if (error != null) {
            log.warn("【网关】权限校验失败，返回403 - path={}, permission={}, error={}", path, permission, error);
            return ResponseWriter.write(exchange,
                    HttpStatus.FORBIDDEN,
                    Result.successResult(403, error));
        }

        // 4. 向下游业务服务传 traceId
        return chain.filter(
                exchange.mutate()
                        .request(r -> r.header("X-Trace-Id", traceId))
                        .build()
        );
//        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}

