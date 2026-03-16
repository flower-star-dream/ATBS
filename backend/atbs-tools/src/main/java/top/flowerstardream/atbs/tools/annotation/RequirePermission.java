package top.flowerstardream.atbs.tools.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author: 花海
 * @Date: 2026/02/09/10:56
 * @Description: 需要权限的注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String value();  // 权限编码，如 "flight:create"
    int clientType() default 0; // 0表示不限制，1后管，2小程序
}