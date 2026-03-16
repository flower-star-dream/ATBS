package top.flowerstardream.atbs.auth.bo.eo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.bo.eo.BaseEO;

import java.io.Serial;

/**
 * 角色表
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("auth_roles")
public class RoleEO extends BaseEO {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("role_code")
    private String roleCode;

    @TableField("role_name")
    private String roleName;

    @TableField("parent_id")
    private Long parentId;

    @TableField("sort")
    private Integer sort;

}
