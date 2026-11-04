package com.indiefund.market.domain.activity.adapter.repository;

import com.indiefund.market.domain.activity.model.entity.UserGameCrowdfundingOrderDetailEntity;
import com.indiefund.market.domain.activity.model.valobj.GameCrowdfundingActivityDiscountVO;
import com.indiefund.market.domain.activity.model.valobj.SCGameTierActivityVO;
import com.indiefund.market.domain.activity.model.valobj.GameTierVO;
import com.indiefund.market.domain.activity.model.valobj.TeamStatisticVO;

import java.util.List;

/**
 * @description 活动仓储
 * @create 2024-12-21 10:06
 */
public interface IActivityRepository {

    GameCrowdfundingActivityDiscountVO queryGameCrowdfundingActivityDiscountVO(Long activityId);

    GameTierVO queryGameTierByGoodsId(String goodsId);

    SCGameTierActivityVO querySCGameTierActivityBySCGoodsId(String source, String channel, String goodsId);

    boolean isTagCrowdRange(String tagId, String userId);

    boolean downgradeSwitch();

    boolean cutRange(String userId);

    List<UserGameCrowdfundingOrderDetailEntity> queryInProgressUserGameCrowdfundingOrderDetailListByOwner(Long activityId, String userId, Integer ownerCount);

    List<UserGameCrowdfundingOrderDetailEntity> queryInProgressUserGameCrowdfundingOrderDetailListByRandom(Long activityId, String userId, Integer randomCount);

    TeamStatisticVO queryTeamStatisticByActivityId(Long activityId);

}
