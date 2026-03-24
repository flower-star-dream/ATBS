package top.flowerstardream.atbs.tools.utils;

import io.jsonwebtoken.Claims;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import top.flowerstardream.atbs.tools.constants.ClientType;
import top.flowerstardream.atbs.tools.constants.JwtClaimsConstant;
import top.flowerstardream.base.properties.JwtProperties;
import top.flowerstardream.base.utils.JwtUtil;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static top.flowerstardream.atbs.tools.constants.RedisPrefixConstant.USER_TOKEN_PREFIX;
import static top.flowerstardream.base.constant.CommonConstant.*;

/**
 * @Author: 花海
 * @Date: 2025/11/04/17:09
 * @Description: JWT 验证工具
 */
@Slf4j
@UtilityClass
public class JwtValidator {

    @Data
    @Builder
    public static class ValidateResult {
        boolean valid;
        ClientType clientType;
        Long userId;
        String username;
        String msg;
        String token;
    }

    public ValidateResult validate(String tokenHeader,
                                   Integer clientTypeHeader,
                                   JwtProperties prop,
                                   StringRedisTemplate redis) {
        // 1. 提取并校验 token
        if (isBlank(tokenHeader)) {
            return ValidateResult.builder().valid(false).msg("未认证").build();
        }
        String rawToken = tokenHeader.startsWith(TOKEN_HEADER)
                ? tokenHeader.substring(TOKEN_HEADER.length())
                : tokenHeader;
        if (isBlank(rawToken)) {
            return ValidateResult.builder().valid(false).msg("token不存在").build();
        }

        // 2. 校验 X-Client-Type
        ClientType clientType;
        clientType = ClientType.fromCode(clientTypeHeader);
        String clientName;
        String secret;
        if (clientType == null) {
            return ValidateResult.builder().valid(false).msg("非法X-Client-Type参数").build();
        } else {
            clientName = clientType.getName();
            secret = prop.getTokens().get(clientName).getSecretKey();
            if (isBlank(secret)) {
                return ValidateResult.builder().valid(false).msg("非法X-Client-Type参数").build();
            }
        }

        // 3. 解析 JWT
        Claims claims = JwtUtil.getClaimsBody(secret, rawToken);
        if (claims == null) {
            log.warn("JWT解析失败");
            return ValidateResult.builder().valid(false).msg("Token无效").build();
        }
        int verify = JwtUtil.verifyToken(claims, prop.getTokens().get(clientName).getRefreshTime());
        if (verify != -1 && verify != 0) {
            return ValidateResult.builder().valid(false).msg("Token过期").build();
        }

        // 4. 取 userId
        String idClaim = JwtClaimsConstant.OPERATOR_ID;
        String usernameClaim = JwtClaimsConstant.OPERATOR_NAME;
        Long userId;
        String username;
        userId = claims.get(idClaim, Long.class);
        username = claims.get(usernameClaim, String.class);

        // 5. Redis 存在性
        String redisKey = USER_TOKEN_PREFIX + userId;
        String redisToken = redis.opsForValue().get(redisKey);
        if (isBlank(redisToken)) {
            return ValidateResult.builder().valid(false).msg("Token未找到").build();
        }

        return ValidateResult.builder().valid(true).clientType(clientType).userId(userId).username(username).token(rawToken).build();
    }
}
