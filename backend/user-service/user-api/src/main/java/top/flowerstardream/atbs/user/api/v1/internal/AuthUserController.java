package top.flowerstardream.atbs.user.api.v1.internal;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.user.ao.req.UserSynchronizeREQ;
import top.flowerstardream.atbs.user.biz.service.IEmployeeService;
import top.flowerstardream.atbs.user.biz.service.IUserService;
import top.flowerstardream.base.result.Result;

/**
 * @Author: 花海
 * @Date: 2026/03/19/04:42
 * @Description: 全局统一用户同步接口
 */
@RestController("internalAuthUserController")
@RequestMapping("/api/internal/v1/user/auth")
@Tag(name = "全局用户服务", description = "全局用户服务")
@Slf4j
public class AuthUserController {

    @Resource
    private IEmployeeService employeeService;

    @Resource
    private IUserService userService;

    /**
     * 同步用户信息
     * @param userSynchronizeREQ
     */
    @GetMapping("/synchronization")
    Result<Void> synchronizationUserInfo(@RequestParam UserSynchronizeREQ userSynchronizeREQ){
        log.info("同步用户信息: {}", userSynchronizeREQ);
        employeeService.synchronize(userSynchronizeREQ);
        userService.synchronize(userSynchronizeREQ);
        return Result.successResult();
    }
}
