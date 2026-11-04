package com.indiefund.market.domain.trade.adapter.repository;

import com.indiefund.market.domain.activity.model.entity.UserGameCrowdfundingOrderDetailEntity;
import com.indiefund.market.domain.trade.model.aggregate.GameCrowdfundingOrderAggregate;
import com.indiefund.market.domain.trade.model.aggregate.GameCrowdfundingRefundAggregate;
import com.indiefund.market.domain.trade.model.aggregate.GameCrowdfundingTeamSettlementAggregate;
import com.indiefund.market.domain.trade.model.entity.GameCrowdfundingActivityEntity;
import com.indiefund.market.domain.trade.model.entity.GameCrowdfundingTeamEntity;
import com.indiefund.market.domain.trade.model.entity.MarketPayOrderEntity;
import com.indiefund.market.domain.trade.model.entity.NotifyTaskEntity;
import com.indiefund.market.domain.trade.model.valobj.GameCrowdfundingProgressVO;

import java.util.List;

/**
 * @description 交易仓储服务接口
 * @create 2025-01-11 09:07
 */
public interface ITradeRepository {

    MarketPayOrderEntity queryMarketPayOrderEntityByOutTradeNo(String userId, String outTradeNo);

    MarketPayOrderEntity lockMarketPayOrder(GameCrowdfundingOrderAggregate gameCrowdfundingOrderAggregate);

    GameCrowdfundingProgressVO queryGameCrowdfundingProgress(String teamId);

    GameCrowdfundingActivityEntity queryGameCrowdfundingActivityEntityByActivityId(Long activityId);

    Integer queryOrderCountByActivityId(Long activityId, String userId);

    GameCrowdfundingTeamEntity queryGameCrowdfundingTeamByTeamId(String teamId);

    NotifyTaskEntity settlementMarketPayOrder(GameCrowdfundingTeamSettlementAggregate gameCrowdfundingTeamSettlementAggregate);

    boolean isSCBlackIntercept(String source, String channel);

    List<NotifyTaskEntity> queryUnExecutedNotifyTaskList();

    List<NotifyTaskEntity> queryUnExecutedNotifyTaskList(String teamId);

    int updateNotifyTaskStatusSuccess(NotifyTaskEntity notifyTaskEntity);

    int updateNotifyTaskStatusError(NotifyTaskEntity notifyTaskEntity);

    int updateNotifyTaskStatusRetry(NotifyTaskEntity notifyTaskEntity);

    boolean occupyTeamStock(String teamStockKey, String recoveryTeamStockKey, Integer target, Integer validTime);

    void recoveryTeamStock(String recoveryTeamStockKey, Integer validTime);

    NotifyTaskEntity unpaid2Refund(GameCrowdfundingRefundAggregate gameCrowdfundingRefundAggregate);

    NotifyTaskEntity paid2Refund(GameCrowdfundingRefundAggregate gameCrowdfundingRefundAggregate);

    NotifyTaskEntity paidTeam2Refund(GameCrowdfundingRefundAggregate gameCrowdfundingRefundAggregate);

    void refund2AddRecovery(String recoveryTeamStockKey, String orderId);

    List<UserGameCrowdfundingOrderDetailEntity> queryTimeoutUnpaidOrderList();

}
