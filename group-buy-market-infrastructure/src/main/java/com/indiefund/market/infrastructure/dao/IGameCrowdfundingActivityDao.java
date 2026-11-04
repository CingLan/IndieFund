package com.indiefund.market.infrastructure.dao;

import com.indiefund.market.infrastructure.dao.po.GameCrowdfundingActivity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @description 众筹活动Dao
 * @create 2024-12-07 10:10
 */
@Mapper
public interface IGameCrowdfundingActivityDao {

    List<GameCrowdfundingActivity> queryGameCrowdfundingActivityList();

    GameCrowdfundingActivity queryValidGameCrowdfundingActivity(GameCrowdfundingActivity gameCrowdfundingActivityReq);

    GameCrowdfundingActivity queryValidGameCrowdfundingActivityId(Long activityId);

    GameCrowdfundingActivity queryGameCrowdfundingActivityByActivityId(Long activityId);

}
