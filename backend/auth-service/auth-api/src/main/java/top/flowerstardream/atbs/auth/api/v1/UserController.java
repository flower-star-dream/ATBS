package top.flowerstardream.atbs.auth.api.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.auth.ao.req.*;
import top.flowerstardream.atbs.auth.biz.service.IAuthUserService;
import top.flowerstardream.atbs.auth.bo.eo.AuthUserEO;
import top.flowerstardream.base.ao.req.BaseStatusChangeREQ;
import top.flowerstardream.base.ao.res.BaseStatusRES;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.result.Result;
import top.flowerstardream.base.state.BaseStatus;

import java.util.List;
import java.util.Map;

import static top.flowerstardream.base.utils.GetInfoUtil.getTraceId;

/**
 * @Author: 花海
 * @Date: 2026/03/18/18:57
 * @Description: 用户控制器
 */
@RestController
@RequestMapping("/api/all/v1/auth/user")
@RequiredArgsConstructor
@Tag(name = "全局用户控制器")
@Slf4j
public class UserController {

    @Resource
    private IAuthUserService userService;

    /**
     * 更新当前登录用户信息
     *
     * @return
     */
    @PutMapping("/info")
    @Operation(summary = "更新当前登录用户信息", description = "更新当前登录用户信息")
    public Result<AuthUserEO> updateInfo(@RequestBody UserInfoREQ userInfoREQ) {
        log.info("【鉴权-用户】traceId:{}, 更新当前登录用户信息：{}", getTraceId(), userInfoREQ);
        userService.updateInfo(userInfoREQ);
        return Result.successResult();
    }

    /**
     * 获取用户列表
     *
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "获取用户列表", description = "获取用户列表")
    public Result<PageResult<AuthUserEO>> list(UserPageQueryREQ userPageQueryREQ) {
        log.info("【鉴权-用户】traceId:{}, 获取用户列表", getTraceId());
        return Result.successResult(userService.list(userPageQueryREQ));
    }

    /**
     * 新增用户账号
     * @param
     * @return
     */
    @PostMapping("/add")
    @Operation(summary = "新增用户账号", description = "新增用户账号")
    public Result<String> add(@RequestBody AuthUserREQ userREQ) {
        log.info("【鉴权-用户】traceId:{}, 新增用户账号：{}", getTraceId(), userREQ);
        userService.add(userREQ);
        return Result.successResult();
    }

    /**
     * 修改用户账号
     * @param
     * @return
     */
    @PutMapping("/update")
    @Operation(summary = "修改用户账号", description = "修改用户账号")
    public Result<String> update(@RequestBody AuthUserREQ userREQ) {
        log.info("【鉴权-用户】traceId:{}, 修改用户账号：{}", getTraceId(), userREQ);
        userService.update(userREQ);
        return Result.successResult();
    }

    /**
     * 启用禁用用户账号
     * @param statusChangeREQ
     * @return
     */
    @PostMapping("/status")
    @Operation(summary = "启用禁用用户账号", description = "启用禁用用户账号")
    public Result<String> startOrStop(@RequestBody BaseStatusChangeREQ<BaseStatus> statusChangeREQ){
        log.info("【鉴权-用户】traceId:{}, 启用禁用用户账号：{}，{}", getTraceId(), statusChangeREQ.getStatus(), statusChangeREQ.getId());
        userService.startOrStop(statusChangeREQ);
        return Result.successResult();
    }

    /**
     * 重置密码
     * @param
     * @return
     */
    @PostMapping("/resetPwd")
    @Operation(summary = "重置密码", description = "重置密码")
    public Result<String> resetPassword(@RequestBody ResetPwdREQ resetPwdREQ){
        log.info("【鉴权-用户】traceId:{}, 重置密码：{}", getTraceId(), resetPwdREQ);
        userService.resetPassword(resetPwdREQ);
        return Result.successResult();
    }

    /**
     * 获取用户状态
     * @return
     */
    @GetMapping("/getStatus")
    @Operation(summary = "获取用户状态", description = "获取用户状态")
    public Result<List<BaseStatusRES<BaseStatus>>> getStatus() {
        log.info("【鉴权-用户】traceId:{}, 获取用户状态", getTraceId());
        return Result.successResult(userService.getStatus());
    }

    /**
     * 同步用户信息
     * @param userSynchronizeREQ
     */
    @Operation(summary = "同步用户信息", description = "同步用户信息接口")
    @GetMapping("/synchronization")
    Result<Long> synchronizationUserInfo(@RequestParam UserSynchronizeREQ userSynchronizeREQ){
        log.info("【鉴权-用户】traceId:{}, 同步用户信息: {}", getTraceId(), userSynchronizeREQ);
        return Result.successResult(userService.synchronize(userSynchronizeREQ));
    }

    /**
     * 批量获取用户名称
     * @param userIds
     * @return
     */
    @PostMapping("/batchNames")
    @Operation(summary = "批量获取用户名称", description = "批量获取用户名称")
    public Result<Map<Long, String>> batchGetNames(@RequestBody List<Long> userIds){
        log.info("【鉴权-用户】traceId:{}, 批量获取用户名称: {}", getTraceId(), userIds);
        return userService.batchGetNames(userIds);
    }
}
