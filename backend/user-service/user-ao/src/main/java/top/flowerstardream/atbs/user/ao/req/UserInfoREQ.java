package top.flowerstardream.atbs.user.ao.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户信息请求
 *
 * @author 花海
 * @date 2025-11-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户信息请求")
public class UserInfoREQ implements Serializable {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户昵称")
    private String nickname;
    
    @Schema(description = "用户头像")
    private String avatar;
    
    @Schema(description = "手机号")
    private String phone;
    
    @Schema(description = "邮箱")
    private String email;
}