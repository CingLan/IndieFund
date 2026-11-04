package com.indiefund.market.domain.activity.model.entity;

import com.indiefund.market.domain.activity.model.valobj.GameCrowdfundingActivityDiscountVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @description 试算结果实体对象（给用户展示众筹可获得的优惠信息）
 * @create 2024-12-14 13:45
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrialBalanceEntity {

    /** 商品ID */
    private String goodsId;
    /** 商品名称 */
    private String goodsName;
    /** 原始价格 */
    private BigDecimal originalPrice;
    // 折扣金额
    private BigDecimal deductionPrice;
    // 支付金额
    private BigDecimal payPrice;
    /** 众筹目标数量 */
    private Integer targetCount;
    /** 众筹开始时间 */
    private Date startTime;
    /** 众筹结束时间 */
    private Date endTime;
    /** 是否可见众筹 */
    private Boolean isVisible;
    /** 是否可参与进团 */
    private Boolean isEnable;

    /** 活动配置信息 */
    private GameCrowdfundingActivityDiscountVO gameCrowdfundingActivityDiscountVO;

}
