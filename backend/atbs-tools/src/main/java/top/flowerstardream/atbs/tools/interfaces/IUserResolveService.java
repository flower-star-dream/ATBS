package top.flowerstardream.atbs.tools.interfaces;

import org.springframework.web.bind.annotation.RequestBody;
import top.flowerstardream.base.result.Result;

import java.util.List;
import java.util.Map;

/**
 * @Author: 花海
 * @Date: 2026/03/21/22:07
 * @Description: 用户解析服务接口
 */
public interface IUserResolveService {
    Result<Map<Long, String>> batchGetNames(@RequestBody List<Long> userIds);
}
