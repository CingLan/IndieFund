package com.indiefund.market.domain.activity.service.discount.impl;

import com.indiefund.market.domain.activity.model.valobj.GameCrowdfundingActivityDiscountVO;
import com.indiefund.market.domain.activity.service.discount.AbstractDiscountCalculateService;
import com.indiefund.market.domain.activity.service.discount.IDiscountCalculateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * @description 直减
 * @create 2024-12-22 09:24
 */
@Slf4j
@Service("ZJ")
public class ZJCalculateService extends AbstractDiscountCalculateService {

    @Override
    public BigDecimal doCalculate(BigDecimal originalPrice, GameCrowdfundingActivityDiscountVO.GameCrowdfundingDiscount gameCrowdfundingDiscount) {
        log.info("优惠策略折扣计算:{}", gameCrowdfundingDiscount.getDiscountType().getCode());

        // 折扣表达式 - 直减为扣减金额
        String marketExpr = gameCrowdfundingDiscount.getMarketExpr();

        // 折扣价格
        BigDecimal deductionPrice = originalPrice.subtract(new BigDecimal(marketExpr));

        // 判断折扣后金额，最低支付1分钱
        if (deductionPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("0.01");
        }

        return deductionPrice;
    }

}
