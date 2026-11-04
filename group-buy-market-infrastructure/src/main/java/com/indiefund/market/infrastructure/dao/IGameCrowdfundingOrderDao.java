package com.indiefund.market.infrastructure.dao;

import com.indiefund.market.infrastructure.dao.po.GameCrowdfundingOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @description 用户拼单
 * @create 2025-01-11 10:33
 */
@Mapper
public interface IGameCrowdfundingOrderDao {

    void insert(GameCrowdfundingOrder gameCrowdfundingOrder);

    int updateAddLockCount(String teamId);

    int updateSubtractionLockCount(String teamId);

    GameCrowdfundingOrder queryGameCrowdfundingProgress(String teamId);

    GameCrowdfundingOrder queryGameCrowdfundingTeamByTeamId(String teamId);

    List<GameCrowdfundingOrder> queryGameCrowdfundingTeamByTeamIds(@Param("teamIds") Set<String> teamIds);

    int updateAddCompleteCount(String teamId);

    int updateOrderStatus2COMPLETE(String teamId);

    List<GameCrowdfundingOrder> queryGameCrowdfundingProgressByTeamIds(@Param("teamIds") Set<String> teamIds);

    Integer queryAllTeamCount(@Param("teamIds") Set<String> teamIds);

    Integer queryAllTeamCompleteCount(@Param("teamIds") Set<String> teamIds);

    Integer queryAllUserCount(@Param("teamIds") Set<String> teamIds);

    int unpaid2Refund(GameCrowdfundingOrder gameCrowdfundingOrderReq);

    int paid2Refund(GameCrowdfundingOrder gameCrowdfundingOrderReq);

    int paidTeam2Refund(GameCrowdfundingOrder gameCrowdfundingOrderReq);

    int paidTeam2RefundFail(GameCrowdfundingOrder gameCrowdfundingOrderReq);

}
