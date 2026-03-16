package top.flowerstardream.atbs.order.biz.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import top.flowerstardream.atbs.order.bo.dto.PassengerDTO;
import top.flowerstardream.atbs.order.bo.dto.UserDTO;
import top.flowerstardream.base.result.Result;

import java.util.List;

/**
 * @Author: 花海
 * @Date: 2024/11/11
 * @Description: 用户服务Feign客户端
 */
@FeignClient(name = "atbs-user", path = "/api/internal/v1/user")
public interface UserClient {

    /**
     * 根据乘车人姓名获取乘车人ID列表
     * @param passengerName 乘车人姓名
     * @return 乘车人ID列表
     */
    @GetMapping("/passenger/by-name")
    Result<List<Long>> getPassengerIdsByName(@RequestParam String passengerName);

    /**
     * 根据乘车人ID列表获取乘车人信息列表
     * @param passengerIds 乘车人ID列表
     * @return 乘车人信息列表
     */
    @PostMapping("/passenger/by-ids")
    Result<List<PassengerDTO>> getPassengerByIds(@RequestParam List<Long> passengerIds);

    /**
     * 根据用户ID列表获取用户信息列表
     * @param userIds 用户ID列表
     * @return 用户信息列表
     */
    @PostMapping("/user/")
    Result<List<UserDTO>> getUserByIds(@RequestParam("userIds") List<Long> userIds);


    @PostMapping("/user/by-name")
    Result<List<Long>> getUserIdsByName(@RequestParam("name") String name);
}