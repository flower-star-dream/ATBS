package top.flowerstardream.atbs.order.bo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author: 花海
 * @Date: 2025/11/14/03:25
 * @Description: 站点信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "站点信息传输对象")
public class StationsDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "站点ID")
    private Long id;

    @Schema(description = "站点名称")
    private String name;
}
