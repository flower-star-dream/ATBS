package top.flowerstardream.atbs.user.bo.eo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.bo.eo.AuditBaseEO;

import java.io.Serial;

/**
 * @Author: 花海
 * @Date: 2025/11/06/20:41
 * @Description: 乘客实体对象
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("atbs_passenger")
public class PassengerEO extends AuditBaseEO {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 真实姓名 */
    @TableField("real_name")
    private String realName ;

    /** 证件类型 */
    @TableField("card_type")
    private String cardType ;

    /** 身份证 */
    @TableField("id_card")
    private String idCard ;

    @TableLogic(value = "0", delval = "1")
    @TableField("deleted")
    protected Boolean deleted;

    @Version
    @TableField(value = "version")
    protected Integer version;
}
