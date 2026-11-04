package com.indiefund.market.domain.activity.service.discount;

import com.indiefund.market.domain.activity.model.valobj.GameCrowdfundingActivityDiscountVO;

import java.math.BigDecimal;

/**
 * @description 折扣计算服务
 * @create 2024-12-22 09:17
 */
public interface IDiscountCalculateService {

    /**
     * 折扣计算
     *
     * @param userId           用户ID
     * @param originalPrice    商品原始价格
     * @param gameCrowdfundingDiscount 折扣计划配置
     * @return 商品优惠价格
     */
    BigDecimal calculate(String userId, BigDecimal originalPrice, GameCrowdfundingActivityDiscountVO.GameCrowdfundingDiscount gameCrowdfundingDiscount);

}
