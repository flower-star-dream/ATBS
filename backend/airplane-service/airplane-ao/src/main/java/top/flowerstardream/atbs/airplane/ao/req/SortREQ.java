package top.flowerstardream.atbs.airplane.ao.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: 花海
 * @Date: 2025/12/01/04:36
 * @Description: 排序请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "排序请求")
public class SortREQ implements Serializable {
    @Schema(description = "路线号")
    private Long routeId;

    @Schema(description = "线路站点ids")
    private List<Long> routeStationsIds;
}
