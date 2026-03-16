package top.flowerstardream.atbs.auth.bo.eo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.bo.eo.AuditBaseEO;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 第三方登录绑定表
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("auth_user_social")
public class UserSocialEO extends AuditBaseEO {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("user_id")
    private Long userId;

    @TableField("platform")
    private String platform;

    @TableField("openid")
    private String openid;

    @TableField("unionid")
    private String unionid;

    @TableField("platform_nickname")
    private String platformNickname;

    @TableField("platform_avatar")
    private String platformAvatar;

    @TableField("access_token")
    private String accessToken;

    @TableField("refresh_token")
    private String refreshToken;

    @TableField("token_expire_time")
    private LocalDateTime tokenExpireTime;

    @TableField("extra_data")
    private String extraData;

    @TableLogic(value = "0", delval = "1")
    @TableField("deleted")
    protected Boolean deleted;
}
