package top.flowerstardream.atbs.airplane.bo.eo;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import top.flowerstardream.base.bo.eo.AuditBaseEO;

import java.io.Serializable;

/**
 * @Author: 花海
 * @Date: 2025/11/06/19:00
 * @Description: 飞机实体对象
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("atbs_airplane")
public class AirplaneEO extends AuditBaseEO {

    // 飞机名称
    @TableField("airplane_name")
    private String airplaneName;

    // 飞机型号
    @TableField("airplane_model")
    private String airplaneModel;

    // 座位数
    @TableField("seat_num")
    private Integer seatNum;

    // 服务年数
    @TableField("service_years")
    private Integer serviceYears;

    @TableLogic(value = "0", delval = "1")
    @TableField("deleted")
    protected Boolean deleted;
}
