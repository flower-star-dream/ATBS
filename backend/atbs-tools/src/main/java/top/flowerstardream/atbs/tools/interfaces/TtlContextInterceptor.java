package top.flowerstardream.atbs.tools.interfaces;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import top.flowerstardream.atbs.tools.constants.ClientType;
import top.flowerstardream.atbs.tools.constants.JwtClaimsConstant;
import top.flowerstardream.base.context.RequestContext;
import top.flowerstardream.base.properties.JwtProperties;
import top.flowerstardream.base.properties.MyGatewayProperties;
import top.flowerstardream.base.utils.JwtUtil;
import top.flowerstardream.base.utils.TtlContextHolder;
import top.flowerstardream.base.utils.WhiteListUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static top.flowerstardream.atbs.tools.constants.TransmitConstant.CLIENT_TYPE;
import static top.flowerstardream.base.constant.CommonConstant.*;

/**
 * @Author: 花海
 * @Date: 2025/11/07/13:21
 * @Description: TTL上下文拦截器
 */
@Component
@Slf4j
public class TtlContextInterceptor implements HandlerInterceptor {

    @Resource
    private JwtProperties jwtProperties;

    @Resource
    private MyGatewayProperties myGatewayProperties;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        // 1. 创建TTL上下文, 设置traceId
        RequestContext ctx = new RequestContext();
        String path = request.getRequestURI();
        String traceId = request.getHeader(OPERATOR_ID);
        log.info("【TTL拦截器】traceId: {}", traceId);
        if (traceId == null) {
            log.warn("【TTL拦截器】traceId为空, 创建traceId");
            traceId = UUID.randomUUID().toString();
        }
        ctx.setTraceId(traceId);
        MDC.put("traceId", traceId);

        // 2. 获取业务端，判断是否OpenFeign调用，是则直接放行
        log.info("【TTL拦截器】获取业务端");
        String clientTypeHeaderStr = request.getHeader(CLIENT_TYPE);
        if (StrUtil.isEmpty(clientTypeHeaderStr)) {
            // 提前跳过白名单
            if (WhiteListUtil.shouldSkip(path, myGatewayProperties.getWhiteList())
                    || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
                log.info("【TTL拦截器】白名单跳过");
                TtlContextHolder.set(ctx);
                return true;
            }
            log.error("【TTL拦截器】业务端为空");
            return false;
        }
        int clientTypeHeader = Integer.parseInt(clientTypeHeaderStr);
        ClientType clientType = ClientType.fromCode(clientTypeHeader);
        log.info("【TTL拦截器】业务端: {}", clientType);
        Map<String, Object> extraData = new HashMap<>();
        extraData.put(JwtClaimsConstant.CLIENT_TYPE, clientType);
        ctx.setExtraData(extraData);
        if (ClientType.SYSTEM.equals(clientType)) {
            String operatorId = request.getHeader(OPERATOR_ID);
            if (operatorId != null) {
                ctx.setOperatorId(Long.parseLong(operatorId));
            }
            String operatorName = request.getHeader(OPERATOR_NAME);
            if (operatorName != null) {
                ctx.setOperatorName(operatorName);
            }
            TtlContextHolder.set(ctx);
            return true;
        }

        // 3. 跳过白名单
        if (WhiteListUtil.shouldSkip(path, myGatewayProperties.getWhiteList())
                || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.info("【TTL拦截器】白名单跳过");
            TtlContextHolder.set(ctx);
            return true;
        }

        // 4. 获取token
        String token = request.getHeader(AUTHORIZATION);
        String secret = jwtProperties.getTokens().get(clientType.getName()).getSecretKey();
        // 5. 解析token
        if (token != null && token.startsWith(TOKEN_HEADER)) {
            Claims claims = JwtUtil.getClaimsBody(secret, token.substring(TOKEN_HEADER.length()));
            // 6. 封装TTL上下文
            String operatorId = JwtClaimsConstant.OPERATOR_ID;
            String operatorName = JwtClaimsConstant.OPERATOR_NAME;
            if (claims != null) {
                Long userId = claims.get(operatorId, Long.class);
                String userName = claims.get(operatorName, String.class);
                if (userId != null) {
                    ctx.setOperatorId(userId);
                } else {
                    log.warn("userId为空");
                }
                if (userName != null) {
                    ctx.setOperatorName(userName);
                } else {
                    log.warn("userName为空");
                }
            }
            ctx.setToken(token);
            // 在TtlContextInterceptor.preHandle()第6步后添加：
            List<String> roles = claims.get(JwtClaimsConstant.ROLES, List.class);
            if (roles != null) {
                extraData.put(JwtClaimsConstant.ROLES, roles);
                ctx.setExtraData(extraData);
            }
            TtlContextHolder.set(ctx);
        }
        // 7. 放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) {
        log.info("【TTL拦截器】afterCompletion执行完毕，上下文已清理");
        TtlContextHolder.clear();   // 必须清理
    }
}