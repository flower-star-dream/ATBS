package top.flowerstardream.atbs.user.api.v1.mgmt;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.user.ao.req.UserPageQueryREQ;
import top.flowerstardream.atbs.user.biz.service.IUserService;
import top.flowerstardream.atbs.user.bo.eo.UserEO;
import top.flowerstardream.base.ao.req.BaseStatusChangeREQ;
import top.flowerstardream.base.ao.res.BaseStatusRES;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.result.Result;
import top.flowerstardream.base.state.BaseStatus;

import java.util.List;

import static top.flowerstardream.base.utils.GetInfoUtil.*;


/**
 * 后管用户控制器
 * <p>
 * 处理后管端的用户相关接口，包括用户列表查询和状态管理
 * </p>
 *
 * @author 花海
 * @date 2025-11-15
 */
@Slf4j
@RestController("mgmtUserController")
@RequestMapping("/api/mgmt/v1/user/user")
@Tag(name = "B端用户管理接口", description = "B端用户管理相关接口")
public class UserController {

    @Resource
    private IUserService userService;

    /**
     * 分页查询用户列表
     * <p>
     * 后管端查询用户列表，支持按用户名、手机号、状态进行筛选
     * </p>
     *
     * @param queryREQ 分页查询参数
     * @return 分页结果
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询用户列表", description = "后管端分页查询用户列表接口")
    public Result<PageResult<UserEO>> list(UserPageQueryREQ queryREQ) {
        log.info("【用户-后管】traceId:{}, 分页查询用户列表，查询条件: {}", getTraceId(), queryREQ);
        PageResult<UserEO> pageResult = userService.list(queryREQ);
        return Result.successResult(pageResult);
    }

    /**
     * 更新用户状态
     * <p>
     * 后管端冻结/解冻用户账号
     * </p>
     *
     * @param statusChangeREQ 状态更新请求参数
     * @return 更新结果
     */
    @PostMapping("/status")
    @Operation(summary = "启用禁用用户账号", description = "后管端冻结/解冻用户账号接口")
    public Result<Void> startOrStop(@RequestBody BaseStatusChangeREQ<BaseStatus> statusChangeREQ) {
        log.info("【用户-后管】traceId:{}, 更新用户状态：状态={}, 用户ID={}", 
                getTraceId(), statusChangeREQ.getStatus(), statusChangeREQ.getId());
        userService.startOrStop(statusChangeREQ);
        return Result.successResult();
    }

    /**
     * 获取用户状态列表
     * <p>
     * 后管端获取用户状态列表
     * </p>
     *
     * @return 用户状态列表
     */
    @GetMapping("/getStatus")
    @Operation(summary = "获取用户状态列表", description = "后管端获取用户状态列表接口")
    public Result<List<BaseStatusRES<BaseStatus>>> getStatus() {
        log.info("【用户-后管】traceId:{}, 获取用户状态", getTraceId());
        List<BaseStatusRES<BaseStatus>> statusRES = userService.getStatus();
        return Result.successResult(statusRES);
    }
}
