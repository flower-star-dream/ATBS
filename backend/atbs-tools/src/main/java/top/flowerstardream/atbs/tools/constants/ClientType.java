package top.flowerstardream.atbs.tools.constants;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import top.flowerstardream.base.exception.BaseExceptionEnum;
import top.flowerstardream.base.interfaces.IClientType;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author: 花海
 * @Date: 2026/02/09/10:39
 * @Description: 客户端类型
 */
@Getter
@AllArgsConstructor
public enum ClientType implements IClientType {
    ADMIN_WEB(1, "后管", "mgmt"),
    APPLET(2, "小程序", "app"),
    SYSTEM(3, "系统", "internal");

    private final Integer code;
    private final String alias;
    private final String name;

    private static final Map<Integer, ClientType> CODE_MAP = Arrays.stream(values())
        .collect(Collectors.toMap(ClientType::getCode, Function.identity()));

    public static ClientType fromCode(int code) {
        return CODE_MAP.get(code);
    }

}