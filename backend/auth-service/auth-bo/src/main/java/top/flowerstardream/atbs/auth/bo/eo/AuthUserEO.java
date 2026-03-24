package top.flowerstardream.atbs.auth.bo.eo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.bo.eo.AuditBaseEO;
import top.flowerstardream.base.state.BaseStatus;
import top.flowerstardream.base.state.StatusAble;

import java.io.Serial;

/**
 * 统一用户身份表
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("auth_users")
public class AuthUserEO extends AuditBaseEO implements StatusAble<BaseStatus> {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("username")
    private String username;

    @TableField("password")
    private String password;

    @TableField("phone")
    private String phone;

    @TableField("email")
    private String email;

    @TableField("status")
    private BaseStatus status;
}
