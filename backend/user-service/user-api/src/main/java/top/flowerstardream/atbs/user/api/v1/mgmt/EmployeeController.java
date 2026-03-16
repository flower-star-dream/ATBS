package top.flowerstardream.atbs.user.api.v1.mgmt;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.user.ao.req.*;
import top.flowerstardream.atbs.user.biz.service.IEmployeeService;
import top.flowerstardream.atbs.user.bo.eo.EmployeeEO;
import top.flowerstardream.base.ao.req.BaseStatusChangeREQ;
import top.flowerstardream.base.ao.res.BaseStatusRES;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.result.Result;
import top.flowerstardream.base.state.BaseStatus;

import java.util.List;

import static top.flowerstardream.base.utils.GetInfoUtil.*;


/**
 * @Author: 花海
 * @Date: 2025/10/26/21:43
 * @Description: 后管员工接口
 */
@RestController
@RequestMapping("/api/mgmt/v1/user/employee")
@Tag(name = "B端员工相关接口", description = "B端员工相关接口")
@Slf4j
public class EmployeeController {

    @Resource
    private IEmployeeService employeeService;

    /**
     * 获取当前登录员工信息
     *
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "获取当前登录员工信息", description = "获取当前登录员工信息")
    public Result<EmployeeEO> getInfo() {
        log.info("【用户-员工】traceId:{}, 获取当前登录员工信息:{}", getTraceId(), getOperatorId());
        EmployeeEO employeeEO = employeeService.getInfo(getOperatorId());
        return Result.successResult(employeeEO);
    }

    /**
     * 更新当前登录员工信息
     *
     * @return
     */
    @PutMapping("/info")
    @Operation(summary = "更新当前登录员工信息", description = "更新当前登录员工信息")
    public Result<EmployeeEO> updateInfo(@RequestBody EmployeeInfoREQ employeeInfoREQ) {
        log.info("【用户-员工】traceId:{}, 更新当前登录员工信息：{}", getTraceId(), employeeInfoREQ);
        employeeService.updateInfo(employeeInfoREQ);
        return Result.successResult();
    }

    /**
     * 获取员工列表
     *
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "获取员工列表", description = "获取员工列表")
    public Result<PageResult<EmployeeEO>> list(EmployeePageQueryREQ employeePageQueryREQ) {
        log.info("【用户-员工】traceId:{}, 获取员工列表", getTraceId());
        return Result.successResult(employeeService.list(employeePageQueryREQ));
    }

    /**
     * 新增员工账号
     * @param
     * @return
     */
    @PostMapping("/add")
    @Operation(summary = "新增员工账号", description = "新增员工账号")
    public Result<String> add(@RequestBody EmployeeREQ employeeREQ) {
        log.info("【用户-员工】traceId:{}, 新增员工账号：{}", getTraceId(), employeeREQ);
        employeeService.add(employeeREQ);
        return Result.successResult();
    }

    /**
     * 修改员工账号
     * @param
     * @return
     */
    @PutMapping("/update")
    @Operation(summary = "修改员工账号", description = "修改员工账号")
    public Result<String> update(@RequestBody EmployeeREQ employeeREQ) {
        log.info("【用户-员工】traceId:{}, 修改员工账号：{}", getTraceId(), employeeREQ);
        log.info("实际注入的Service实现类={}", employeeService.getClass().getName());
        employeeService.update(employeeREQ);
        return Result.successResult();
    }

    /**
     * 批量删除员工账号
     * @param ids
     * @return
     */
    @DeleteMapping("/{ids}")
    @Operation(summary = "删除员工账号", description = "删除员工账号")
    public Result<String> delete(@PathVariable List<Long> ids){
        log.info("【用户-员工】traceId:{}, 删除员工账号：{}", getTraceId(), ids);
        employeeService.delete(ids);
        return Result.successResult();
    }


    /**
     * 启用禁用员工账号
     * @param statusChangeREQ
     * @return
     */
    @PostMapping("/status")
    @Operation(summary = "启用禁用员工账号", description = "启用禁用员工账号")
    public Result<String> startOrStop(@RequestBody BaseStatusChangeREQ<BaseStatus> statusChangeREQ){
        log.info("【用户-员工】traceId:{}, 启用禁用员工账号：{}，{}", getTraceId(), statusChangeREQ.getStatus(), statusChangeREQ.getId());
        employeeService.startOrStop(statusChangeREQ);
        return Result.successResult();
    }

    /**
     * 获取员工状态
     * @return
     */
    @GetMapping("/getStatus")
    @Operation(summary = "获取员工状态", description = "获取员工状态")
    public Result<List<BaseStatusRES<BaseStatus>>> getStatus() {
        log.info("【用户-员工】traceId:{}, 获取员工状态", getTraceId());
        List<BaseStatusRES<BaseStatus>> statusRES = employeeService.getStatus();
        return Result.successResult(statusRES);
    }

}
