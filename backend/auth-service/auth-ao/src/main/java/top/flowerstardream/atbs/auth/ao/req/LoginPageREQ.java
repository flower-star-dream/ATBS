package top.flowerstardream.atbs.auth.ao.req;

import lombok.Builder;
import lombok.Data;

/**
 * 登录页面请求参数
 * 用于后端托管登录页面的表单提交
 * @author 花海
 * @date 2026/03/29
 * @description 登录页面请求参数
 */
@Data
@Builder
public class LoginPageREQ {
    /**
     * 用户名/手机号/邮箱
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 客户端ID
     */
    private String clientId;

    /**
     * 重定向URI
     */
    private String redirectUri;

    /**
     * 状态码（防CSRF）
     */
    private String state;

    /**
     * 请求范围
     */
    private String scope;

    /**
     * PKCE Code Challenge
     */
    private String codeChallenge;

    /**
     * PKCE Code Challenge Method
     */
    private String codeChallengeMethod;
}
