package top.flowerstardream.atbs.user.api.v1.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.user.ao.req.PassengerREQ;
import top.flowerstardream.atbs.user.ao.res.PassengerRES;
import top.flowerstardream.atbs.user.biz.service.IPassengerService;
import top.flowerstardream.base.result.Result;

import java.util.List;

import static top.flowerstardream.base.utils.GetInfoUtil.*;

/**
 * 小程序乘客控制器
 * <p>
 * 处理小程序端的乘客相关接口
 * </p>
 *
 * @author 花海
 * @date 2025-11-15
 */
@RestController("appPassengerController")
@RequestMapping("/api/app/v1/user/passenger")
@Tag(name = "C端乘客相关接口", description = "C端乘客相关接口")
@Slf4j
public class PassengerController {

    @Resource
    private IPassengerService passengerService;

    /**
     * 获取当前用户关联的乘客列表
     * <p>
     * 根据用户ID获取所有关联的乘客信息
     * </p>
     *
     * @return 乘客列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取当前用户关联的乘客列表", description = "小程序端获取当前用户关联的乘客列表接口")
    public Result<List<PassengerRES>> getUserPassengers() {
        log.info("【用户-乘客】traceId:{}, 获取当前用户关联的乘客列表，用户ID: {}", getTraceId(), getOperatorId());
        List<PassengerRES> passengerList = passengerService.getUserPassengers(getOperatorId());
        return Result.successResult(passengerList);
    }

    /**
     * 新增乘客
     * <p>
     * 新增乘客信息并关联到当前用户，若身份证号存在则仅保存关联关系
     * </p>
     *
     * @param passengerREQ 乘客信息
     * @return 操作结果
     */
    @PostMapping("/add")
    @Operation(summary = "新增乘客", description = "小程序端新增乘客并关联到当前用户接口")
    public Result<String> addPassenger(@RequestBody PassengerREQ passengerREQ) {
        log.info("【用户-乘客】traceId:{}, 新增乘客，用户ID: {}, 乘客信息: {}", getTraceId(), getOperatorId(), passengerREQ);
        passengerService.addPassenger(getOperatorId(), passengerREQ);
        return Result.successResult();
    }

    /**
     * 设置默认乘客
     * <p>
     * 将指定的乘客设置为当前用户的默认乘客，要求该乘客已关联到当前用户
     * </p>
     *
     * @param passengerId 乘客ID
     * @return 操作结果
     */
    @PutMapping("/default/{id}")
    @Operation(summary = "设置默认乘客", description = "小程序端设置当前用户默认乘客接口")
    public Result<String> setDefaultPassenger(@PathVariable("id") Long passengerId) {
        log.info("【用户-乘客】traceId:{}, 设置默认乘客，用户ID: {}, 乘客ID: {}", getTraceId(), getOperatorId(), passengerId);
        passengerService.setDefaultPassenger(getOperatorId(), passengerId);
        return Result.successResult();
    }
}
