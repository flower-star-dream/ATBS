package top.flowerstardream.atbs.user.api.v1.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.user.ao.req.UserInfoREQ;
import top.flowerstardream.atbs.user.biz.service.IUserService;
import top.flowerstardream.atbs.user.bo.eo.UserEO;
import top.flowerstardream.base.result.Result;

import java.util.Collections;
import java.util.List;

import static top.flowerstardream.base.utils.GetInfoUtil.*;

/**
 * 小程序用户控制器
 * <p>
 * 处理小程序端的用户相关接口
 * </p>
 *
 * @author 花海
 * @date 2025-11-15
 */
@RestController("appUserController")
@RequestMapping("/api/app/v1/user/user")
@Tag(name = "C端用户相关接口", description = "C端用户相关接口")
@Slf4j
public class UserController {

    @Resource
    private IUserService userService;

    /**
     * 获取用户信息
     * <p>
     * 根据用户ID获取用户详细信息
     * </p>
     *
     * @return 用户信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取用户信息", description = "小程序端获取用户信息接口")
    public Result<UserEO> getUserInfo() {
        log.info("【用户-小程序】traceId:{}, 获取用户信息，用户ID: {}", getTraceId(), getOperatorId());
        List<UserEO> userEO = userService.getUserInfo(Collections.singletonList(getOperatorId()));
        return Result.successResult(userEO.get(0));
    }

    /**
     * 更新用户信息
     * <p>
     * 更新用户的昵称、头像、手机号、邮箱等信息
     * </p>
     *
     * @param userInfoREQ 用户信息更新请求参数
     * @return 更新结果
     */
    @PutMapping("/info")
    @Operation(summary = "更新用户信息", description = "小程序端更新用户信息接口")
    public Result<String> updateUserInfo(@RequestBody UserInfoREQ userInfoREQ) {
        log.info("【用户-小程序】traceId:{}, 更新用户信息，请求参数: {}", getTraceId(), userInfoREQ);
        userService.updateUserInfo(userInfoREQ);
        return Result.successResult();
    }
}
