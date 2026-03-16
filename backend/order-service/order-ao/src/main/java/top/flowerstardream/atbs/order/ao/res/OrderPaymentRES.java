package top.flowerstardream.atbs.order.ao.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author: 花海
 * @Date: 2025/11/11
 * @Description: 订单支付结果
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaymentRES implements Serializable {
    //随机字符串
    private String nonceStr;
    //签名
    private String paySign;
    //时间戳
    private String timeStamp;
    //签名算法
    private String signType;
    //统一下单接口返回的 prepay_id 参数值
    private String packageStr;

}
