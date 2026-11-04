package com.indiefund.market.domain.trade.service.settlement.filter;

import com.indiefund.market.domain.trade.model.entity.GameCrowdfundingTeamEntity;
import com.indiefund.market.domain.trade.model.entity.TradeSettlementRuleCommandEntity;
import com.indiefund.market.domain.trade.model.entity.TradeSettlementRuleFilterBackEntity;
import com.indiefund.market.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @description 结束节点
 * @create 2025-01-29 16:37
 */
@Slf4j
@Service
public class EndRuleFilter implements ILogicHandler<TradeSettlementRuleCommandEntity, TradeSettlementRuleFilterFactory.DynamicContext, TradeSettlementRuleFilterBackEntity> {

    @Override
    public TradeSettlementRuleFilterBackEntity apply(TradeSettlementRuleCommandEntity requestParameter, TradeSettlementRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("结算规则过滤-结束节点{} outTradeNo:{}", requestParameter.getUserId(), requestParameter.getOutTradeNo());

        // 获取上下文对象
        GameCrowdfundingTeamEntity gameCrowdfundingTeamEntity = dynamicContext.getGameCrowdfundingTeamEntity();

        // 返回封装数据
        return TradeSettlementRuleFilterBackEntity.builder()
                .teamId(gameCrowdfundingTeamEntity.getTeamId())
                .activityId(gameCrowdfundingTeamEntity.getActivityId())
                .targetCount(gameCrowdfundingTeamEntity.getTargetCount())
                .completeCount(gameCrowdfundingTeamEntity.getCompleteCount())
                .lockCount(gameCrowdfundingTeamEntity.getLockCount())
                .status(gameCrowdfundingTeamEntity.getStatus())
                .validStartTime(gameCrowdfundingTeamEntity.getValidStartTime())
                .validEndTime(gameCrowdfundingTeamEntity.getValidEndTime())
                .notifyConfigVO(gameCrowdfundingTeamEntity.getNotifyConfigVO())
                .build();
    }

}
