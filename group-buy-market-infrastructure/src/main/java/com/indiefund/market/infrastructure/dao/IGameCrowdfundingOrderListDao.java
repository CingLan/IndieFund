package com.indiefund.market.infrastructure.dao;

import com.indiefund.market.infrastructure.dao.po.GameCrowdfundingOrderList;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @description 用户拼单明细
 * @create 2025-01-11 09:07
 */
@Mapper
public interface IGameCrowdfundingOrderListDao {

    void insert(GameCrowdfundingOrderList gameCrowdfundingOrderListReq);

    GameCrowdfundingOrderList queryGameCrowdfundingOrderRecordByOutTradeNo(GameCrowdfundingOrderList gameCrowdfundingOrderListReq);

    Integer queryOrderCountByActivityId(GameCrowdfundingOrderList gameCrowdfundingOrderListReq);

    int updateOrderStatus2COMPLETE(GameCrowdfundingOrderList gameCrowdfundingOrderListReq);

    List<String> queryGameCrowdfundingCompleteOrderOutTradeNoListByTeamId(String teamId);

    List<GameCrowdfundingOrderList> queryInProgressUserGameCrowdfundingOrderDetailListByUserId(GameCrowdfundingOrderList gameCrowdfundingOrderListReq);

    List<GameCrowdfundingOrderList> queryInProgressUserGameCrowdfundingOrderDetailListByRandom(GameCrowdfundingOrderList gameCrowdfundingOrderListReq);

    List<GameCrowdfundingOrderList> queryInProgressUserGameCrowdfundingOrderDetailListByActivityId(Long activityId);

    int unpaid2Refund(GameCrowdfundingOrderList gameCrowdfundingOrderListReq);

    int paid2Refund(GameCrowdfundingOrderList gameCrowdfundingOrderListReq);

    int paidTeam2Refund(GameCrowdfundingOrderList gameCrowdfundingOrderListReq);

    /**
     * 查询超时未支付订单列表
     * 条件：当前时间不在活动时间范围内、状态为0（初始锁定）、out_trade_time为空
     * @return 超时未支付订单列表，限制10条
     */
    List<GameCrowdfundingOrderList> queryTimeoutUnpaidOrderList();

}
