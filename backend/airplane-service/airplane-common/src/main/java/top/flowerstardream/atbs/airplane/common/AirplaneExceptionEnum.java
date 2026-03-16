package top.flowerstardream.atbs.airplane.common;


import lombok.AllArgsConstructor;
import lombok.Getter;
import top.flowerstardream.base.exception.IExceptionEnum;

/**
 * @Author: 花海
 * @Date: 2025/11/21 18:05
 * @Description: 座位异常枚举
 */
@Getter
@AllArgsConstructor
public enum AirplaneExceptionEnum implements IExceptionEnum {

    ROUTE_IS_USED(20001, "路线已被使用，无法被删除"),
    ROUTE_AlREADY_EXISTS(20002, "路线已存在"),
    SCHEDULE_IS_USED(20003, "班次已被使用，无法被删除"),
    SCHEDULE_ALREADY_EXISTS(20004, "班次已存在"),

    ROUTE_STATIONS_ALREADY_EXISTS(20006, "路线站点已存在"),
    SEAT_RESERVATION_IS_USED(20007, "座位已被预定"),
    SEAT_RESERVATION_ALREADY_EXISTS(20008, "座位预订已存在"),
    STATION_IS_USED(20009, "站点已被使用，无法被删除"),
    STATION_ALREADY_EXISTS(20010, "站点已存在"),
    AIRPLANE_IS_USED(20011, "飞机已被使用，无法被删除"),
    AIRPLANE_ALREADY_EXISTS(20012, "飞机已存在"),
    THE_SORTING_INFORMATION_STATION_CANNOT_BE_FOUND(20013, "无法找到起始站或终点站的排序信息"),
    THE_TERMINAL_STATION_IS_LOCATED_BEFORE_THE_STARTING_STATION(20014, "终点站位于起点站之前"),
    NOT_ENOUGH_SEATS(20015, "没有足够的座位"),
    NOT_ENOUGH_TICKETS(20016, "没有足够的余票"),
    ROUTE_NOT_EXIST(20017, "路线不存在"),
    INCORRECT_SORTING(20018, "错误的排序"),
    THE_AIRPLANE_NOT_EXIST(20019, "飞机不存在");
    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误信息
     */
    private final String message;

    public static AirplaneExceptionEnum valueOf(Integer code) {
        for (AirplaneExceptionEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

}
