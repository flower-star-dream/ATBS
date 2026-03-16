package top.flowerstardream.atbs.user.api.v1.mgmt;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.user.ao.req.PassengerPageQueryREQ;
import top.flowerstardream.atbs.user.biz.service.IPassengerService;
import top.flowerstardream.atbs.user.bo.eo.PassengerEO;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.result.Result;

import static top.flowerstardream.base.utils.GetInfoUtil.*;

/**
 * @Author: 花海
 * @Date: 2025-11-10
 * @Description: 后管乘客接口
 */
@RestController("mgmtPassengerController")
@RequestMapping("/api/mgmt/v1/user/passenger")
@Tag(name = "B端乘客相关接口", description = "B端乘客相关接口")
@Slf4j
public class PassengerController {

    @Resource
    private IPassengerService passengerService;

    /**
     * 分页查询乘客列表
     *
     * @param queryREQ 查询条件
     * @return 乘客列表分页结果
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询乘客列表", description = "根据条件分页查询乘客信息")
    public Result<PageResult<PassengerEO>> pageQuery(PassengerPageQueryREQ queryREQ) {
        log.info("【用户-乘客】traceId:{},分页查询乘客列表，查询条件：{}", getTraceId(), queryREQ);
        PageResult<PassengerEO> result = passengerService.pageQuery(queryREQ);
        return Result.successResult(result);
    }

    /**
     * 根据ID查询乘客详情
     *
     * @param id 乘客ID
     * @return 乘客详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询乘客详情", description = "根据ID查询乘客详细信息")
    public Result<PassengerEO> getById(@PathVariable Long id) {
        log.info("【用户-乘客】traceId:{},查询乘客详情，ID：{}", getTraceId(), id);
        PassengerEO passenger = passengerService.query(id);
        return Result.successResult(passenger);
    }
}
