package top.flowerstardream.atbs.auth.bo.eo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.bo.eo.AuditBaseEO;

import java.io.Serial;

/**
 * 权限定义表
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("auth_permissions")
public class PermissionEO extends AuditBaseEO {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("perm_code")
    private String permCode;

    @TableField("perm_name")
    private String permName;

    @TableField("type")
    private Integer type;

    @TableField("parent_id")
    private Long parentId;

    @TableField("sort")
    private Integer sort;

    @TableField("icon")
    private String icon;

    @TableField("path")
    private String path;

    @TableField("component")
    private String component;
}
