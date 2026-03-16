package top.flowerstardream.atbs.user.bo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 乘客数据传输对象
 *
 * @author 花海
 * @date 2025-11-10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "乘客数据传输对象")
public class PassengerDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "乘客ID")
    private Long id;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "证件类型")
    private String cardType;

    @Schema(description = "身份证号")
    private String idCard;
}