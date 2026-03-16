package top.flowerstardream.atbs.auth.bo.eo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.bo.eo.BaseEO;

import java.io.Serial;

/**
 * 角色权限关联表
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("auth_role_permissions")
public class RolePermissionEO extends BaseEO {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("role_id")
    private Long roleId;

    @TableField("permission_id")
    private Long permissionId;
}
