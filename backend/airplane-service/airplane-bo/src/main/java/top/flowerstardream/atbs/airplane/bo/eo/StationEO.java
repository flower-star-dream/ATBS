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
 * @Description: 路线实体对象
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("atbs_station")
public class StationEO extends AuditBaseEO implements Serializable {

    // 站点名称
    @TableField("station_name")
    private String stationName;

    // 站点地址
    @TableField("address")
    private String address;

    @TableLogic(value = "0", delval = "1")
    @TableField("deleted")
    protected Boolean deleted;
}
