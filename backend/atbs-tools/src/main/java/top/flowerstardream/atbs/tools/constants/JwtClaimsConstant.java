package top.flowerstardream.atbs.tools.constants;

import jakarta.annotation.Resource;
import lombok.Getter;
import org.springframework.stereotype.Component;
import top.flowerstardream.base.properties.JwtProperties;

/**
 * @Author: 花海
 * @Date: 2025/10/26/22:01
 * @Description: JWT claims常量
 */
@Getter
public class JwtClaimsConstant {
    public final static String OPERATOR_ID = "operatorId";
    public final static String OPERATOR_NAME = "operatorName";
    public final static String CLIENT_TYPE = "clientType";
    public final static String ROLES = "roles";
}
