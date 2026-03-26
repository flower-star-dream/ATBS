package top.flowerstardream.atbs.user.bo.eo;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * @Author: 花海
 * @Date: 2025/10/26/21:00
 * @Description: 员工实体对象
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("atbs_employee")
public class EmployeeEO extends BizBaseEO<BaseStatus> implements StatusAble<BaseStatus> {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    @TableField(value = "id", fill = FieldFill.INSERT)
    protected Long id;

    // 用户名
    @TableField("username")
    private String username ;

    // 昵称
    @TableField("nickname")
    private String nickname ;

    // 权限等级
    @TableField("permission_level")
    private String permissionLevel ;

    // 头像
    @TableField("avatar")
    private String avatar ;

    // 手机号
    @TableField("phone")
    private String phone ;

    // 所属站点
    @TableField("affiliated_site")
    private String affiliatedSite ;
}