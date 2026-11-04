package com.indiefund.market.domain.trade.model.entity;

import com.indiefund.market.domain.trade.model.valobj.NotifyConfigVO;
import com.indiefund.market.domain.trade.model.valobj.NotifyTypeEnumVO;
import com.indiefund.market.types.enums.GameCrowdfundingOrderEnumVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @description 众筹组队实体对象
 * @create 2025-01-26 16:19
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameCrowdfundingTeamEntity {

    /** 拼单组队ID */
    private String teamId;
    /** 活动ID */
    private Long activityId;
    /** 目标数量 */
    private Integer targetCount;
    /** 完成数量 */
    private Integer completeCount;
    /** 锁单数量 */
    private Integer lockCount;
    /** 状态（0-拼单中、1-完成、2-失败） */
    private GameCrowdfundingOrderEnumVO status;
    /** 众筹开始时间 - 参与众筹时间 */
    private Date validStartTime;
    /** 众筹结束时间 - 众筹有效时长 */
    private Date validEndTime;
    /** 回调配置 */
    private NotifyConfigVO notifyConfigVO;

}
