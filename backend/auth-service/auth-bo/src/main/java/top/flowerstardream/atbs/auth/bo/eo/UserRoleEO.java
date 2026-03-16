package top.flowerstardream.atbs.auth.bo.eo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.bo.eo.AuditBaseEO;

import java.io.Serial;

/**
 * 用户角色关联表
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("auth_user_roles")
public class UserRoleEO extends AuditBaseEO {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("user_id")
    private Long userId;

    @TableField("role_id")
    private Long roleId;

}
