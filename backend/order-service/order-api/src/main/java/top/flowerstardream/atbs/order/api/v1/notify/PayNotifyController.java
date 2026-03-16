package top.flowerstardream.atbs.order.api.v1.notify;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.core.exception.DecryptionException;
import com.wechat.pay.java.core.exception.ValidationException;
import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.flowerstardream.atbs.order.biz.service.IOrderService;
import top.flowerstardream.base.properties.WeChatProperties;

import java.io.BufferedReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: 花海
 * @Date: 2025/11/11
 * @Description: 支付回调相关接口
 */
@RestController
@RequestMapping("/notify")
@Slf4j
public class PayNotifyController {

    @Resource
    private IOrderService orderService;

    @Resource
    private WeChatProperties weChatProperties;

    private NotificationParser notificationParser;

    /**
     * 初始化通知解析器
     */
    @PostConstruct
    public void init() {
        // 使用 RSAAutoCertificateConfig 自动管理平台证书
        NotificationConfig config = new RSAAutoCertificateConfig.Builder()
                .merchantId(weChatProperties.getMchid())
                .privateKeyFromPath(weChatProperties.getPrivateKeyFilePath())
                .merchantSerialNumber(weChatProperties.getMchSerialNo())
                .apiV3Key(weChatProperties.getApiV3Key())
                .build();

        this.notificationParser = new NotificationParser(config);
        log.info("微信支付通知解析器初始化完成");
    }

    /**
     * 支付成功回调
     *
     * @param request
     * @param response
     */
    @RequestMapping("/paySuccess")
    public void paySuccessNotify(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 1. 获取请求头参数（用于验签）
            String serialNo = request.getHeader("Wechatpay-Serial");
            String nonce = request.getHeader("Wechatpay-Nonce");
            String timestamp = request.getHeader("Wechatpay-Timestamp");
            String signature = request.getHeader("Wechatpay-Signature");

            // 2. 获取请求体
            String body = readData(request);
            log.info("收到微信支付回调，请求体：{}", body);

            // 3. 构建 RequestParam
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(serialNo)
                    .nonce(nonce)
                    .signType("WECHATPAY2-SHA256-RSA2048")
                    .timestamp(timestamp)
                    .signature(signature)
                    .body(body)
                    .build();

            // 4. 解析并验签（自动解密）
            // Transaction 是支付通知的通用模型，包含 JSAPI、Native、H5 等所有支付场景
            Transaction transaction = notificationParser.parse(requestParam, Transaction.class);

            // 5. 获取订单信息
            // 商户订单号
            String outTradeNo = transaction.getOutTradeNo();
            // 微信支付订单号
            String transactionId = transaction.getTransactionId();
            // 订单总金额（分）
            Integer totalAmount = transaction.getAmount().getTotal();

            log.info("支付回调验签成功，商户订单号：{}，微信交易号：{}，金额：{}分",
                    outTradeNo, transactionId, totalAmount);

            // 6. 业务处理（注意：金额转换为元或保持分，根据你的业务定，这里保持和原代码一致用分）
            BigDecimal amount = new BigDecimal(totalAmount);
            orderService.paySuccess(outTradeNo, amount);

            // 7. 响应微信成功
            responseToWeixin(response, HttpStatus.OK, "SUCCESS", "成功");

        } catch (ValidationException e) {
            // 验签失败（可能请求被篡改）
            log.error("微信支付回调验签失败", e);
            responseToWeixin(response, HttpStatus.UNAUTHORIZED, "FAIL", "验签失败");
        } catch (DecryptionException e) {
            // 解密失败
            log.error("微信支付回调解密失败", e);
            responseToWeixin(response, HttpStatus.BAD_REQUEST, "FAIL", "解密失败");
        } catch (Exception e) {
            // 其他异常（业务处理异常等）
            log.error("微信支付回调处理异常", e);
            // 返回 500 让微信重试，或者根据业务决定是否返回 SUCCESS
            responseToWeixin(response, HttpStatus.INTERNAL_SERVER_ERROR, "FAIL", "系统错误");
        }
    }

    /**
     * 读取请求数据
     *
     * @param request
     * @return
     * @throws Exception
     */
    private String readData(HttpServletRequest request) throws Exception {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!result.isEmpty()) {
                    result.append("\n");
                }
                result.append(line);
            }
        }
        return result.toString();
    }

    /**
     * 给微信响应
     *
     * @param response
     * @param status   HTTP 状态码
     * @param code     业务码 SUCCESS/FAIL
     * @param message  消息
     */
    private void responseToWeixin(HttpServletResponse response, HttpStatus status, String code, String message) {
        try {
            response.setStatus(status.value());
            response.setHeader("Content-Type", "application/json; charset=utf-8");

            Map<String, String> map = new HashMap<>();
            map.put("code", code);
            map.put("message", message);

            String json = JSON.toJSONString(map);
            response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
            response.flushBuffer();
        } catch (Exception e) {
            log.error("响应微信回调失败", e);
        }
    }

    /**
     * 退款成功回调（如果需要）
     * 与支付回调类似，使用 RefundNotification 类解析
     */
    @RequestMapping("/refundSuccess")
    public void refundSuccessNotify(HttpServletRequest request, HttpServletResponse response) {
        try {
            String serialNo = request.getHeader("Wechatpay-Serial");
            String nonce = request.getHeader("Wechatpay-Nonce");
            String timestamp = request.getHeader("Wechatpay-Timestamp");
            String signature = request.getHeader("Wechatpay-Signature");
            String body = readData(request);

            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(serialNo)
                    .nonce(nonce)
                    .signType("WECHATPAY2-SHA256-RSA2048")
                    .timestamp(timestamp)
                    .signature(signature)
                    .body(body)
                    .build();

            // 退款通知使用 RefundNotification 类
            com.wechat.pay.java.service.refund.model.RefundNotification refundNotification =
                    notificationParser.parse(requestParam, com.wechat.pay.java.service.refund.model.RefundNotification.class);

            String outTradeNo = refundNotification.getOutTradeNo();
            String outRefundNo = refundNotification.getOutRefundNo();
            String refundStatus = refundNotification.getRefundStatus().toString();

            log.info("退款回调成功，商户订单号：{}，退款单号：{}，状态：{}",
                    outTradeNo, outRefundNo, refundStatus);

            // TODO: 处理退款业务逻辑

            responseToWeixin(response, HttpStatus.OK, "SUCCESS", "成功");

        } catch (Exception e) {
            log.error("退款回调处理失败", e);
            responseToWeixin(response, HttpStatus.INTERNAL_SERVER_ERROR, "FAIL", "处理失败");
        }
    }
}