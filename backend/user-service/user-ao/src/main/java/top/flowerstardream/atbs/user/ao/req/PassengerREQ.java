package top.flowerstardream.atbs.user.ao.req;

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
 * @Description: 乘客请求对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "乘客请求对象")
public class PassengerREQ implements Serializable {

    @Schema(description = "真实姓名")
    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    @Schema(description = "证件类型")
    @NotBlank(message = "证件类型不能为空")
    private String cardType;

    @Schema(description = "身份证号")
    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^\\d{17}[0-9Xx]$", message = "身份证号格式不正确")
    private String idCard;
}