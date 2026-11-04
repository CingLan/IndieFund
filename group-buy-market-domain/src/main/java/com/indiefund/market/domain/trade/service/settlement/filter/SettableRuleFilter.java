package com.indiefund.market.domain.trade.service.settlement.filter;

import com.indiefund.market.domain.trade.adapter.repository.ITradeRepository;
import com.indiefund.market.domain.trade.model.entity.GameCrowdfundingTeamEntity;
import com.indiefund.market.domain.trade.model.entity.MarketPayOrderEntity;
import com.indiefund.market.domain.trade.model.entity.TradeSettlementRuleCommandEntity;
import com.indiefund.market.domain.trade.model.entity.TradeSettlementRuleFilterBackEntity;
import com.indiefund.market.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import com.indiefund.market.types.enums.ResponseCode;
import com.indiefund.market.types.exception.AppException;
import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @description 可结算规则过滤；交易时间
 * @create 2025-01-29 09:38
 */
@Slf4j
@Service
public class SettableRuleFilter implements ILogicHandler<TradeSettlementRuleCommandEntity, TradeSettlementRuleFilterFactory.DynamicContext, TradeSettlementRuleFilterBackEntity> {

    @Resource
    private ITradeRepository repository;

    @Override
    public TradeSettlementRuleFilterBackEntity apply(TradeSettlementRuleCommandEntity requestParameter, TradeSettlementRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("结算规则过滤-有效时间校验{} outTradeNo:{}", requestParameter.getUserId(), requestParameter.getOutTradeNo());

        // 上下文；获取数据
        MarketPayOrderEntity marketPayOrderEntity = dynamicContext.getMarketPayOrderEntity();

        // 查询众筹对象
        GameCrowdfundingTeamEntity gameCrowdfundingTeamEntity = repository.queryGameCrowdfundingTeamByTeamId(marketPayOrderEntity.getTeamId());

        // 外部交易时间 - 也就是用户支付完成的时间，这个时间要在众筹有效时间范围内
        Date outTradeTime = requestParameter.getOutTradeTime();

        // 判断，外部交易时间，要小于众筹结束时间。否则抛异常。
        if (!outTradeTime.before(gameCrowdfundingTeamEntity.getValidEndTime())) {
            log.error("订单交易时间不在众筹有效时间范围内");
            throw new AppException(ResponseCode.E0106);
        }

        // 设置上下文
        dynamicContext.setGameCrowdfundingTeamEntity(gameCrowdfundingTeamEntity);

        return next(requestParameter, dynamicContext);
    }

}
