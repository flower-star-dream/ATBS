package top.flowerstardream.atbs.user.api.v1.internal;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.user.bo.dto.PassengerDTO;
import top.flowerstardream.atbs.user.biz.service.IPassengerService;
import top.flowerstardream.base.result.Result;

import java.util.List;

import static top.flowerstardream.base.utils.GetInfoUtil.*;


/**
 * @Author: 花海
 * @Date: 2025/11/21/20:54
 * @Description: 乘客服务
 */
@RestController("internalPassengerController")
@RequestMapping("/api/internal/v1/user/passenger")
@Tag(name = "乘客服务", description = "乘客服务")
@Slf4j
public class PassengerController {

    @Resource
    private IPassengerService passengerService;

    /**
     * 根据乘客姓名获取乘客ID列表
     * @param passengerName 乘客姓名
     * @return 乘客ID列表
     */
    @GetMapping("/by-name")
    Result<List<Long>> getPassengerIdsByName(@RequestParam String passengerName){
        log.info("【用户-乘客】traceId:{}, 根据乘客姓名获取乘客ID列表, 乘客姓名: {}", getTraceId(), passengerName);
        List<Long> passengers = passengerService.getPassengersByName(passengerName);
        return Result.successResult(passengers);
    }

    /**
     * 根据乘客ID列表获取乘客信息列表
     * @param passengerIds 乘客ID列表
     * @return 乘客信息列表
     */
    @PostMapping("/by-ids")
    Result<List<PassengerDTO>> getPassengerByIds(@RequestParam List<Long> passengerIds){
        log.info("【用户-乘客】traceId:{}, 根据乘客ID列表获取乘客信息列表, 乘客ID列表: {}", getTraceId(), passengerIds);
        List<PassengerDTO> passengers = passengerService.getPassengersByIds(passengerIds);
        return Result.successResult(passengers);
    }
}
