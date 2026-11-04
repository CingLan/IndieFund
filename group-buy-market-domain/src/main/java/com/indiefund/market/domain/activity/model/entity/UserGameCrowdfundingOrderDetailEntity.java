package com.indiefund.market.domain.activity.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @description 众筹组队实体对象
 * @create 2025-02-02 13:10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserGameCrowdfundingOrderDetailEntity {

    /** 用户ID */
    private String userId;
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
    /** 众筹开始时间 - 参与众筹时间 */
    private Date validStartTime;
    /** 众筹结束时间 - 众筹有效时长 */
    private Date validEndTime;
    /** 外部交易单号-确保外部调用唯一幂等 */
    private String outTradeNo;
    /** 渠道 */
    private String source;
    /** 来源 */
    private String channel;

}
