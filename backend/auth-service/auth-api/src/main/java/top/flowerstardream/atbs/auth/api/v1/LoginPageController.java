package top.flowerstardream.atbs.auth.api.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.auth.ao.req.LoginPageREQ;
import top.flowerstardream.atbs.auth.biz.service.ILoginPageService;

import java.io.IOException;

import static top.flowerstardream.atbs.auth.common.OAuth2Constant.*;

/**
 * 登录页面控制器
 * 提供后端托管的登录页面和登录处理
 * @Author: 花海
 * @Date: 2026/03/29
 * @Description: 登录页面控制器
 */
@Controller
@RequestMapping
@RequiredArgsConstructor
@Slf4j
@Tag(name = "登录页面")
public class LoginPageController {

    @Resource
    private ILoginPageService loginPageService;

    /**
     * 登录页面 - 后端托管的登录表单页面
     * GET /login?client_id=xxx&redirect_uri=xxx&scope=xxx&state=xxx&code_challenge=xxx&code_challenge_method=xxx
     *
     * @param clientId           客户端ID
     * @param redirectUri        重定向URI
     * @param scope              请求范围
     * @param state              状态码（防CSRF）
     * @param codeChallenge      PKCE Code Challenge
     * @param codeChallengeMethod PKCE Code Challenge Method
     * @param error              错误码（登录失败时）
     * @param errorDescription   错误描述（登录失败时）
     * @param model              Spring MVC Model
     * @return 登录页面视图名称
     */
    @Operation(summary = "登录页面")
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = CLIENT_ID, required = false) String clientId,
            @RequestParam(value = REDIRECT_URI, required = false) String redirectUri,
            @RequestParam(value = SCOPE, required = false) String scope,
            @RequestParam(value = STATE, required = false) String state,
            @RequestParam(value = CODE_CHALLENGE, required = false) String codeChallenge,
            @RequestParam(value = CODE_CHALLENGE_METHOD, required = false) String codeChallengeMethod,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription,
            Model model
    ) {
        // 将OAuth2参数传递给页面，用于表单提交
        model.addAttribute(CLIENT_ID, clientId);
        model.addAttribute(REDIRECT_URI, redirectUri);
        model.addAttribute(SCOPE, scope);
        model.addAttribute(STATE, state);
        model.addAttribute(CODE_CHALLENGE, codeChallenge);
        model.addAttribute(CODE_CHALLENGE_METHOD, codeChallengeMethod);

        // 传递错误信息（如果有）
        model.addAttribute("error", error);
        model.addAttribute("errorDescription", errorDescription);

        log.debug("访问登录页面 - clientId: {}, redirectUri: {}, hasError: {}", clientId, redirectUri, error != null);
        return "login";
    }

    /**
     * 登录处理 - 处理登录表单提交
     * POST /login
     * Content-Type: application/x-www-form-urlencoded
     *
     * @param req      登录请求参数
     * @param response HTTP响应对象
     * @throws IOException 重定向时可能抛出IO异常
     */
    @Operation(summary = "登录处理")
    @PostMapping(
            value = "/login",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public void doLogin(
            @ModelAttribute LoginPageREQ req,
            HttpServletResponse response
    ) throws IOException {
        log.debug("处理登录请求 - username: {}, clientId: {}", req.getUsername(), req.getClientId());
        loginPageService.handleLogin(req, response);
    }

    /**
     * 授权码回调页面 - 用于前端应用接收授权码
     * GET /oauth/callback?code=xxx&state=xxx
     *
     * @param code  授权码
     * @param state 状态码
     * @param model Spring MVC Model
     * @return 回调页面视图名称
     */
    @Operation(summary = "授权码回调页面")
    @GetMapping("/oauth/callback")
    public String callbackPage(
            @RequestParam(value = CODE, required = false) String code,
            @RequestParam(value = STATE, required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            Model model
    ) {
        model.addAttribute(CODE, code);
        model.addAttribute(STATE, state);
        model.addAttribute("error", error);

        log.debug("访问回调页面 - code: {}, state: {}, error: {}", code, state, error);
        return "callback";
    }
}
