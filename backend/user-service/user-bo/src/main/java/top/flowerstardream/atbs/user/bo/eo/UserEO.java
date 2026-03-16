package top.flowerstardream.atbs.user.bo.eo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.bo.eo.BizBaseEO;
import top.flowerstardream.base.state.BaseStatus;
import top.flowerstardream.base.state.StatusAble;

import java.io.Serial;
import java.io.Serializable;


/**
 *
 * @author 花海
 * @date 2025-10-14
 * @Description: 用户实体对象
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("atbs_user")
public class UserEO extends BizBaseEO<BaseStatus> implements StatusAble<BaseStatus> {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    @TableField(value = "id", fill = FieldFill.INSERT)
    protected Long id;

    @TableField("openid")
    private String openid;

    // 用户名
    @TableField("username")
    private String username;

    // 头像
    @TableField("avatar")
    private String avatar;

    // 邮箱
    @TableField("email")
    private String email;

    // 手机号
    @TableField("phone")
    private String phone;

    // 乘客id
    @TableField("passenger_id")
    private Long passengerId ;

}