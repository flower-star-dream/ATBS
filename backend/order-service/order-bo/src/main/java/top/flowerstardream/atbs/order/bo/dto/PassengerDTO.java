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
 * @Date: 2025/11/11
 * @Description: 乘车人传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "乘车人传输对象")
public class PassengerDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "乘车人ID")
    private Long id;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "证件类型")
    private String cardType;

    @Schema(description = "身份证号")
    private String idCard;
}