package top.flowerstardream.atbs.auth.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.flowerstardream.base.exception.IExceptionEnum;

/**
 * @Author: 花海
 * @Date: 2026/03/15/17:05
 * @Description: 认证异常枚举
 */
@Getter
@AllArgsConstructor
public enum AuthExceptionEnum implements IExceptionEnum {

    THE_JWT_CONFIGURATION_NOT_FOUND(20001, "JWT配置未找到"),
    THE_JWT_TOKEN_INVALID(20002, "JWT令牌无效"),
    THE_JWT_TOKEN_HAS_EXPIRED(20003, "JWT令牌已过期");

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误信息
     */
    private final String message;

    public static AuthExceptionEnum valueOf(Integer code) {
        for (AuthExceptionEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
