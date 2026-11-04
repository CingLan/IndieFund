package com.indiefund.market.domain.trade.service.refund.business.impl;

import com.indiefund.market.domain.trade.model.aggregate.GameCrowdfundingRefundAggregate;
import com.indiefund.market.domain.trade.model.entity.GameCrowdfundingTeamEntity;
import com.indiefund.market.domain.trade.model.entity.NotifyTaskEntity;
import com.indiefund.market.domain.trade.model.entity.TradeRefundOrderEntity;
import com.indiefund.market.domain.trade.model.valobj.TeamRefundSuccess;
import com.indiefund.market.domain.trade.service.lock.factory.TradeLockRuleFilterFactory;
import com.indiefund.market.domain.trade.service.refund.business.AbstractRefundOrderStrategy;
import com.indiefund.market.types.enums.GameCrowdfundingOrderEnumVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 发起退单（已成团&已支付），锁单量-1、完成量-1、组队订单状态更新、发送退单消息（MQ）
 *
 * 2025/7/8 07:45
 */
@Slf4j
@Service("paidTeam2RefundStrategy")
public class PaidTeam2RefundStrategy extends AbstractRefundOrderStrategy {

    @Override
    public void refundOrder(TradeRefundOrderEntity tradeRefundOrderEntity) {
        log.info("退单；已支付，已成团 userId:{} teamId:{} orderId:{}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getTeamId(), tradeRefundOrderEntity.getOrderId());

        GameCrowdfundingTeamEntity gameCrowdfundingTeamEntity = repository.queryGameCrowdfundingTeamByTeamId(tradeRefundOrderEntity.getTeamId());
        Integer completeCount = gameCrowdfundingTeamEntity.getCompleteCount();

        // 最后一笔也退单，则更新众筹订单为失败
        GameCrowdfundingOrderEnumVO gameCrowdfundingOrderEnumVO = 1 == completeCount ? GameCrowdfundingOrderEnumVO.FAIL : GameCrowdfundingOrderEnumVO.COMPLETE_FAIL;

        // 1. 退单，已支付&已成团
        NotifyTaskEntity notifyTaskEntity = repository.paidTeam2Refund(GameCrowdfundingRefundAggregate.buildPaidTeam2RefundAggregate(tradeRefundOrderEntity, -1, -1, gameCrowdfundingOrderEnumVO));

        // 2. 发送MQ消息 - 发送MQ，恢复锁单库存量使用
        sendRefundNotifyMessage(notifyTaskEntity, "已支付，已成团");

    }

    @Override
    public void reverseStock(TeamRefundSuccess teamRefundSuccess) throws Exception {
        log.info("退单；已支付、已成团，队伍组队结束，不需要恢复锁单量 {} {} {}", teamRefundSuccess.getUserId(), teamRefundSuccess.getActivityId(), teamRefundSuccess.getTeamId());
    }

}
