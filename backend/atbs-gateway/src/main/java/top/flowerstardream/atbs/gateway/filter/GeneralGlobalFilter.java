package top.flowerstardream.atbs.gateway.filter;

import cn.hutool.core.lang.UUID;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import top.flowerstardream.atbs.tools.utils.JwtValidator;
import top.flowerstardream.base.properties.JwtProperties;
import top.flowerstardream.base.properties.MyGatewayProperties;
import top.flowerstardream.base.result.Result;
import top.flowerstardream.base.utils.ResponseWriter;

import java.util.Objects;

import static top.flowerstardream.atbs.tools.constants.TransmitConstant.*;
import static top.flowerstardream.base.constant.CommonConstant.*;

/**
 * @Author: 花海
 * @Date: 2026/02/09/18:39
 * @Description: 全局过滤器
 */
@Component
@Order(0)
@Slf4j
@RequiredArgsConstructor
public class GeneralGlobalFilter implements GlobalFilter {

    @Resource
    private JwtProperties jwtProperties;
    
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private MyGatewayProperties myGatewayProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = UUID.randomUUID().toString();
        log.info("【网关】生成 traceId={}", traceId);
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String requestMethod = request.getMethod().name();

        // 1. 白名单 & OPTIONS
        boolean isWhitelisted = isWhiteList(path);
        boolean isOptions = "OPTIONS".equalsIgnoreCase(requestMethod);
        log.info("【网关】白名单检查 - path={}, isWhitelisted={}, isOptions={}", path, isWhitelisted, isOptions);

        if (isWhitelisted || isOptions) {
            log.info("【网关】请求被白名单放行或为OPTIONS请求");
            return chain.filter(exchange);
        }

        log.info("【网关】请求未被白名单匹配，进入JWT校验流程");

        // 2. JWT 校验
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        Integer clientTypeHeader = Integer.valueOf(Objects.requireNonNull(request.getHeaders().getFirst(CLIENT_TYPE)));

        log.info("【网关】准备进行JWT校验 - authHeader={}, clientTypeHeader={}",
                 authHeader != null ? "存在" : "不存在",
                 clientTypeHeader);

        JwtValidator.ValidateResult vr = JwtValidator.validate(
                authHeader,
                clientTypeHeader,
                jwtProperties,
                stringRedisTemplate);

        log.info("【网关】JWT校验完成 - valid={}, msg={}", vr.isValid(), vr.getMsg());

        if (!vr.isValid()) {
            log.warn("【网关】JWT校验失败，返回401 - path={}, msg={}", path, vr.getMsg());
            return ResponseWriter.write(exchange,
                    HttpStatus.UNAUTHORIZED,
                    Result.successResult(401, vr.getMsg()));
        }
        Integer clientType = vr.getClientType().getCode();
        Long userId = vr.getUserId();
        String username = vr.getUsername();

        // 4. 将用户信息传递到下游服务（通过Header）
        ServerHttpRequest mutatedRequest = request.mutate()
            .header(TRACE_ID, traceId)
            .header(OPERATOR_ID, String.valueOf(userId))
            .header(OPERATOR_NAME, username)
            .header(CLIENT_TYPE, String.valueOf(clientType))
            .build();
            
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * 判断是否在白名单中
     * @param path 请求路径
     * @return boolean
     */
    private boolean isWhiteList(String path) {
        return myGatewayProperties.getWhiteList().stream().anyMatch(path::startsWith);
    }
}