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

    INVALID_REQUEST(20001, "无效的请求"),
    UNAUTHORIZED_CLIENT(20002, "未经授权的客户端"),
    ACCESS_DENIED(20003, "拒绝访问"),
    UNSUPPORTED_RESPONSE_TYPE(20004, "不支持的响应类型"),
    INVALID_SCOPE(20005, "无效的范围"),
    INSUFFICIENT_SCOPE(20006, "范围不足"),
    INVALID_TOKEN(20007, "无效的令牌"),
    SERVER_ERROR(20008, "服务器错误"),
    TEMPORARILY_UNAVAILABLE(20009, "暂时不可用"),
    INVALID_CLIENT(20010, "无效的客户端"),
    INVALID_GRANT(20011, "无效的授权"),
    UNSUPPORTED_GRANT_TYPE(20012, "不受支持的授权类型"),
    UNSUPPORTED_TOKEN_TYPE(20013, "不支持的令牌类型"),
    INVALID_REDIRECT_URI(20014, "无效的重定向uri"),
    INVALID_ENDPOINT(20015, "无效的端点"),
    JWT_CONFIG_NOT_FOUND(20016, "JWT配置未找到"),
    USER_NOT_EXIST(20017, "用户不存在"),
    UNSUPPORTED_CODE_CHALLENGE_METHODS(20018, "不支持的PKCE扩展方法"),
    OPENID_CANNOT_BE_EMPTY(20019, "openid不可为空"),
    USER_ALREADY_EXISTS(20020, "用户已存在"),
    USER_PASSWORD_ERROR(20021, "用户名或密码错误"),
    USER_STATUS_ENABLE(20022, "启用状态用户禁止删除"),
    THE_OLD_PASSWORD_CANNOT_EQUALS_NEW_ONE(20023, "新密码不能与旧密码相同"),
    THE_NEW_PASSWORD_IS_INCONSISTENT_WITH_THE_CONFIRMED_PASSWORD(20024, "新密码与确认密码不一致"),
    THE_ACCOUNT_HAS_BEEN_DISABLED(20025, "账户已禁用"),
    THE_OLD_PASSWORD_IS_INCORRECT(20026, "旧密码错误"),
    DATA_DOES_NOT_EXIST(20027, "数据不存在"),
    THIRD_PARTY_DATA_DOES_NOT_EXIST(20028, "第三方数据不存在");

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
