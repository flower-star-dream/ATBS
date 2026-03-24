package top.flowerstardream.atbs.auth.bo.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 授权码数据
 * @author 花海
 * @date 2026/03/18/09:05
 * @description 授权码数据
 */
@Data
@Builder
public class AuthorizationCodeData {
    private String code;
    private Long userId;
    private String clientId;
    private String redirectUri;
    private String scope;
    private String codeChallenge;
    private String codeChallengeMethod;
    private LocalDateTime createTime;
}