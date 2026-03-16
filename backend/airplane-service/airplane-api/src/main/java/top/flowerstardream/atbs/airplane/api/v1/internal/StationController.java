package top.flowerstardream.atbs.airplane.api.v1.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.airplane.bo.dto.StationsDTO;
import top.flowerstardream.atbs.airplane.biz.service.IStationService;
import top.flowerstardream.base.result.Result;

import java.util.List;

/**
 * @Author: QAQ
 * @Date: 2025/11/26 17:19
 * @Description: 站点数据服务接口（供其他服务调用）
 */

@RestController("internalStationController")
@RequestMapping("/api/internal/v1/airplane/station")
@Tag(name = "站点数据服务")
@Slf4j
public class StationController {

    @Resource
    private IStationService stationService;


    @Operation(summary = "根据站名获取站ID", description = "站点数据服务接口，根据站名获取站ID")
    @GetMapping("/by-name")
    public Result<List<Long>> getStationIdsByName(@RequestParam("stationName") String stationName){
        log.info("【站点数据服务】根据站名获取站ID，站名: {}", stationName);
        List<Long> stationIds = stationService.getStationIdsByName(stationName);
        return Result.successResult(stationIds);
    }

    /*
     * 功能是用于查站名，但是要求返回的是DTO
     */
    @Operation(summary = "根据站ID获取站名", description = "站点数据服务接口，根据站ID获取站名")
    @PostMapping("/by-ids")
    public Result<List<StationsDTO>> getStationNamesByStationIds(@RequestParam List<Long> stationIds){
        log.info("【站点数据服务】根据站ID获取站名，站ID: {}", stationIds);
        List<StationsDTO> stationDTOs = stationService.getStationDTOsByStationIds(stationIds);
        return Result.successResult(stationDTOs);
    }
}
