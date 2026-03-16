package top.flowerstardream.atbs.user.ao.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.ao.req.BasePageQueryREQ;

import java.io.Serializable;

/**
 * 乘客分页查询请求
 *
 * @author 花海
 * @date 2025-11-10
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "乘客分页查询请求")
public class PassengerPageQueryREQ extends BasePageQueryREQ{

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "证件类型")
    private String cardType;

    @Schema(description = "身份证号")
    private String idCard;
}