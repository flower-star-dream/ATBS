package top.flowerstardream.atbs.auth.ao.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 授权码响应
 * 用于登录成功后返回授权码给前端
 * @author 花海
 * @date 2026/03/29
 * @description 授权码响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationCodeRES implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 授权码
     */
    private String code;

    /**
     * 状态码
     */
    private String state;

    /**
     * 重定向URI
     */
    private String redirectUri;
}
