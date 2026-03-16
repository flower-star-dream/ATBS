package top.flowerstardream.atbs.user.bo.eo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.bo.eo.BaseEO;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author: 花海
 * @Date: 2025/11/11/00:59
 * @Description: 用户乘客实体对象
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("atbs_user_passenger")
public class UserPassengerEO extends BaseEO{
    @Serial
    private static final long serialVersionUID = 1L;

    // 用户ID
    @TableField("user_id")
    private long userId ;

    // 乘客ID
    @TableField("passenger_id")
    private long passengerId ;
}
