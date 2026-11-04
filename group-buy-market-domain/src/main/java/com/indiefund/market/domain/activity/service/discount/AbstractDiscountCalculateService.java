package com.indiefund.market.domain.activity.service.discount;

import com.indiefund.market.domain.activity.adapter.repository.IActivityRepository;
import com.indiefund.market.domain.activity.model.valobj.DiscountTypeEnum;
import com.indiefund.market.domain.activity.model.valobj.GameCrowdfundingActivityDiscountVO;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * @description 折扣计算服务抽象类
 * @create 2024-12-22 12:32
 */
@Slf4j
public abstract class AbstractDiscountCalculateService implements IDiscountCalculateService {

    @Resource
    protected IActivityRepository repository;

    @Override
    public BigDecimal calculate(String userId, BigDecimal originalPrice, GameCrowdfundingActivityDiscountVO.GameCrowdfundingDiscount gameCrowdfundingDiscount) {
        // 1. 人群标签过滤
        if (DiscountTypeEnum.TAG.equals(gameCrowdfundingDiscount.getDiscountType())){
            boolean isCrowdRange = filterTagId(userId, gameCrowdfundingDiscount.getTagId());
            if (!isCrowdRange) {
                log.info("折扣优惠计算拦截，用户不再优惠人群标签范围内 userId:{}", userId);
                return originalPrice;
            }
        }
        // 2. 折扣优惠计算
        return doCalculate(originalPrice, gameCrowdfundingDiscount);
    }

    // 人群过滤 - 限定人群优惠
    private boolean filterTagId(String userId, String tagId) {
        return repository.isTagCrowdRange(tagId, userId);
    }

    protected abstract BigDecimal doCalculate(BigDecimal originalPrice, GameCrowdfundingActivityDiscountVO.GameCrowdfundingDiscount gameCrowdfundingDiscount);

}
