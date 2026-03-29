package top.flowerstardream.atbs.auth.biz.service;

import jakarta.servlet.http.HttpServletResponse;
import top.flowerstardream.atbs.auth.ao.req.LoginPageREQ;

import java.io.IOException;

/**
 * 登录页面服务接口
 * 处理后端托管登录页面的业务逻辑
 * @author 花海
 * @date 2026/03/29
 * @description 登录页面服务接口
 */
public interface ILoginPageService {

    /**
     * 处理登录请求
     * 验证用户凭据，生成授权码，并重定向到回调地址
     *
     * @param req      登录请求参数
     * @param response HTTP响应对象
     * @throws IOException 重定向时可能抛出IO异常
     */
    void handleLogin(LoginPageREQ req, HttpServletResponse response) throws IOException;
}
