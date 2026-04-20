package top.flowerstardream.atbs.tools.interfaces;

import cn.hutool.core.lang.UUID;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.flowerstardream.atbs.tools.constants.ClientType;
import top.flowerstardream.atbs.tools.constants.JwtClaimsConstant;
import top.flowerstardream.base.context.RequestContext;
import top.flowerstardream.base.utils.TtlContextHolder;

import static top.flowerstardream.atbs.tools.constants.TransmitConstant.CLIENT_TYPE;
import static top.flowerstardream.base.constant.CommonConstant.*;
import static top.flowerstardream.base.utils.GetInfoUtil.*;

/**
 * @Author: 花海
 * @Date: 2025/11/12/17:05
 * @Description: Feign拦截器
 */
@Component
@Slf4j
public class FeignInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        String traceId = getTraceId() == null ? MDC.get("traceId") : getTraceId();
        if (traceId == null) {
            traceId = UUID.randomUUID().toString(true);
        }

        /* 1. 必须带的 */
        template.header(TRACE_ID, traceId);

        /* 2. 你想额外塞的任意字段 */
        template.header(OPERATOR_ID, getOperatorId() == null ? "" : getOperatorId().toString());
        template.header(OPERATOR_NAME, getOperatorName());
        template.header(CLIENT_TYPE, ClientType.SYSTEM.getCode().toString());

        /* 3. 传递Authorization token */
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader(AUTHORIZATION);
            if (authHeader != null && !authHeader.isEmpty()) {
                template.header(AUTHORIZATION, authHeader);
            }
        }

        log.debug("【Feign 发送请求】url={}, headers={}", template.url(), template.headers());
    }
}