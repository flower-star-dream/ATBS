package top.flowerstardream.atbs.user.common;

import static top.flowerstardream.atbs.tools.constants.RedisPrefixConstant.USER_BUSINESS_PREFIX;

/**
 * @Author: 花海
 * @Date: 2025/11/09/20:29
 * @Description: 用户Redis前缀常量
 */
public class UserRedisPrefixConstant {
    public static final String EMPLOYEE_BUSINESS_PREFIX = "%semployee:".formatted(USER_BUSINESS_PREFIX);
    public static final String USERS_BUSINESS_PREFIX = "%suser:".formatted(USER_BUSINESS_PREFIX);

    // 用于Spring Cache的缓存名称
    public static final String EMPLOYEE_INFO_CACHE_NAME = "user:employee:info:aid:";
    public static final String USER_INFO_CACHE_NAME = "user:user:info:uid:";

}