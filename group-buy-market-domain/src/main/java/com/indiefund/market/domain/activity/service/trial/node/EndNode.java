package com.indiefund.market.domain.activity.service.trial.node;

import com.indiefund.market.domain.activity.model.entity.MarketProductEntity;
import com.indiefund.market.domain.activity.model.entity.TrialBalanceEntity;
import com.indiefund.market.domain.activity.model.valobj.GameCrowdfundingActivityDiscountVO;
import com.indiefund.market.domain.activity.model.valobj.GameTierVO;
import com.indiefund.market.domain.activity.service.trial.AbstractGameCrowdfundingMarketSupport;
import com.indiefund.market.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * @description 正常结束节点
 * @create 2024-12-14 14:31
 */
@Slf4j
@Service
public class EndNode extends AbstractGameCrowdfundingMarketSupport<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {

    @Override
    public TrialBalanceEntity doApply(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("众筹商品查询试算服务-EndNode userId:{} requestParameter:{}", requestParameter.getUserId(), JSON.toJSONString(requestParameter));

        // 众筹活动配置
        GameCrowdfundingActivityDiscountVO gameCrowdfundingActivityDiscountVO = dynamicContext.getGameCrowdfundingActivityDiscountVO();

        // 商品信息
        GameTierVO gameTierVO = dynamicContext.getGameTierVO();

        // 折扣金额
        BigDecimal deductionPrice = dynamicContext.getDeductionPrice();
        // 支付金额
        BigDecimal payPrice = dynamicContext.getPayPrice();

        // 返回空结果
        return TrialBalanceEntity.builder()
                .goodsId(gameTierVO.getGoodsId())
                .goodsName(gameTierVO.getGoodsName())
                .originalPrice(gameTierVO.getOriginalPrice())
                .deductionPrice(deductionPrice)
                .payPrice(payPrice)
                .targetCount(gameCrowdfundingActivityDiscountVO.getTarget())
                .startTime(gameCrowdfundingActivityDiscountVO.getStartTime())
                .endTime(gameCrowdfundingActivityDiscountVO.getEndTime())
                .isVisible(dynamicContext.isVisible())
                .isEnable(dynamicContext.isEnable())
                .gameCrowdfundingActivityDiscountVO(gameCrowdfundingActivityDiscountVO)
                .build();
    }

    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return defaultStrategyHandler;
    }

}
