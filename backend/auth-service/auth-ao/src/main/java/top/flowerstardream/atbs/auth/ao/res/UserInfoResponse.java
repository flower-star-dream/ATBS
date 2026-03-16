package top.flowerstardream.atbs.auth.ao.res;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息响应DTO
 */
@Data
@Builder
public class UserInfoResponse {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 用户类型
     */
    private Integer userType;

    /**
     * 是否超级管理员
     */
    private Integer isSuperAdmin;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 角色列表
     */
    private List<String> roles;

    /**
     * 权限列表
     */
    private List<String> permissions;
}
