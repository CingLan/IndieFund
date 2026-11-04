package com.indiefund.market.domain.trade.service.refund.filter;

import com.indiefund.market.domain.trade.adapter.repository.ITradeRepository;
import com.indiefund.market.domain.trade.model.entity.GameCrowdfundingTeamEntity;
import com.indiefund.market.domain.trade.model.entity.MarketPayOrderEntity;
import com.indiefund.market.domain.trade.model.entity.TradeRefundBehaviorEntity;
import com.indiefund.market.domain.trade.model.entity.TradeRefundCommandEntity;
import com.indiefund.market.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import com.indiefund.market.domain.trade.service.refund.factory.TradeRefundRuleFilterFactory;
import com.indiefund.market.types.enums.GameCrowdfundingOrderEnumVO;
import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 数据节点
 *
 * 2025/7/30 09:58
 */
@Slf4j
@Service
public class DataNodeFilter implements ILogicHandler<TradeRefundCommandEntity, TradeRefundRuleFilterFactory.DynamicContext, TradeRefundBehaviorEntity> {

    @Resource
    private ITradeRepository repository;

    @Override
    public TradeRefundBehaviorEntity apply(TradeRefundCommandEntity tradeRefundCommandEntity, TradeRefundRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("逆向流程-退单操作，数据加载节点 userId:{} outTradeNo:{}", tradeRefundCommandEntity.getUserId(), tradeRefundCommandEntity.getOutTradeNo());

        // 1. 查询外部交易单，组队id、orderId、众筹状态
        MarketPayOrderEntity marketPayOrderEntity = repository.queryMarketPayOrderEntityByOutTradeNo(tradeRefundCommandEntity.getUserId(), tradeRefundCommandEntity.getOutTradeNo());
        String teamId = marketPayOrderEntity.getTeamId();

        // 2. 查询众筹状态
        GameCrowdfundingTeamEntity gameCrowdfundingTeamEntity = repository.queryGameCrowdfundingTeamByTeamId(teamId);

        // 3. 写入上下文；如果查询数据是比较多的，可以参考 MarketNode2CompletableFuture 通过多线程进行加载
        dynamicContext.setMarketPayOrderEntity(marketPayOrderEntity);
        dynamicContext.setGameCrowdfundingTeamEntity(gameCrowdfundingTeamEntity);

        return next(tradeRefundCommandEntity, dynamicContext);
    }

}
