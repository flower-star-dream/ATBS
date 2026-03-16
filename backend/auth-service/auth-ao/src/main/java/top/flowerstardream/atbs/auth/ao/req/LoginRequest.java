package top.flowerstardream.atbs.auth.ao.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求DTO
 */
@Data
public class LoginRequest {

    /**
     * 用户名/手机号/邮箱
     */
    @NotBlank(message = "账号不能为空")
    private String account;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 客户端标识
     */
    @NotBlank(message = "客户端标识不能为空")
    private String clientId;

    /**
     * 验证码（可选）
     */
    private String captcha;

    /**
     * 验证码key（可选）
     */
    private String captchaKey;
}
