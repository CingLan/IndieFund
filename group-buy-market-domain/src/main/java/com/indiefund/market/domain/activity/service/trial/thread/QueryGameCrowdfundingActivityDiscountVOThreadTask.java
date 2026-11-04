package com.indiefund.market.domain.activity.service.trial.thread;

import com.indiefund.market.domain.activity.adapter.repository.IActivityRepository;
import com.indiefund.market.domain.activity.model.valobj.GameCrowdfundingActivityDiscountVO;
import com.indiefund.market.domain.activity.model.valobj.SCGameTierActivityVO;

import java.util.concurrent.Callable;

/**
 * @description 查询营销配置任务
 * @create 2024-12-21 09:46
 */
public class QueryGameCrowdfundingActivityDiscountVOThreadTask implements Callable<GameCrowdfundingActivityDiscountVO> {

    /**
     * 活动ID
     */
    private final Long activityId;

    /**
     * 来源
     */
    private final String source;

    /**
     * 渠道
     */
    private final String channel;

    /**
     * 商品ID
     */
    private final String goodsId;

    /**
     * 活动仓储
     */
    private final IActivityRepository activityRepository;

    public QueryGameCrowdfundingActivityDiscountVOThreadTask(Long activityId, String source, String channel, String goodsId, IActivityRepository activityRepository) {
        this.activityId = activityId;
        this.source = source;
        this.channel = channel;
        this.goodsId = goodsId;
        this.activityRepository = activityRepository;
    }

    @Override
    public GameCrowdfundingActivityDiscountVO call() throws Exception {
        // 判断是否存在可用的活动ID
        Long availableActivityId = activityId;
        if (null == activityId){
            // 查询渠道商品活动配置关联配置
            SCGameTierActivityVO scGameTierActivityVO = activityRepository.querySCGameTierActivityBySCGoodsId(source, channel, goodsId);
            if (null == scGameTierActivityVO) return null;
            availableActivityId = scGameTierActivityVO.getActivityId();
        }
        // 查询活动配置
        return activityRepository.queryGameCrowdfundingActivityDiscountVO(availableActivityId);
    }

}
