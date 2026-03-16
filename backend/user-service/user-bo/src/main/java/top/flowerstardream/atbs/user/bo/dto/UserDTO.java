package top.flowerstardream.atbs.user.bo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author 花海
 * @date 2025-10-14
 * @description 用户数据传输对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "用户数据传输对象")
public class UserDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户id")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "openId")
    private String openId;

}