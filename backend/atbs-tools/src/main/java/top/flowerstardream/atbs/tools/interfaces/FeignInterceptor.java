package top.flowerstardream.atbs.tools.interfaces;

import cn.hutool.core.lang.UUID;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import top.flowerstardream.base.context.RequestContext;
import top.flowerstardream.base.utils.TtlContextHolder;

import static top.flowerstardream.atbs.tools.constants.TransmitConstant.CLIENT_TYPE;
import static top.flowerstardream.base.constant.CommonConstant.*;

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
        RequestContext ctx = TtlContextHolder.get();
        String traceId = ctx == null ? MDC.get("traceId") : ctx.getTraceId();
        if (traceId == null) {
            traceId = UUID.randomUUID().toString(true);
        }

        /* 1. 必须带的 */
        template.header(TRACE_ID, traceId);

        /* 2. 你想额外塞的任意字段 */
        template.header(OPERATOR_ID, ctx == null ? "" : ctx.getOperatorId().toString());
        template.header(OPERATOR_NAME, ctx == null ? "" : ctx.getOperatorName());
        template.header(CLIENT_TYPE, ctx == null ? "" : ctx.getExtra(CLIENT_TYPE, Integer.class).toString());

        log.debug("【Feign 发送请求】url={}, headers={}", template.url(), template.headers());
    }
}