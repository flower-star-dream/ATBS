package top.flowerstardream.atbs.user.ao.res;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author: 花海
 * @Date: 2025/11/11
 * @Description: 乘客响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "乘客请求对象")
public class PassengerRES implements Serializable {

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "证件类型")
    private String cardType;

    @Schema(description = "身份证号")
    private String idCard;
}