package top.flowerstardream.atbs.auth.common;

/**
 * @Author: 花海
 * @Date: 2026/03/17/17:04
 * @Description: 认证服务Redis前缀常量
 */
public class AuthRedisPrefixConstant {
    public static final String USER_TOKEN_PREFIX = "oauth2:user:login:token:uid:";
    public static final String OAUTH2_CODE_PREFIX = "oauth2:code:";
    public static final String OAUTH2_REFRESH_PREFIX = "oauth2:refresh:";
    public static final String OAUTH2_USER_TOKEN_PREFIX = "oauth2:user:token:";
    public static final String OAUTH2_BLACKLIST_PREFIX = "oauth2:blacklist:";
}
