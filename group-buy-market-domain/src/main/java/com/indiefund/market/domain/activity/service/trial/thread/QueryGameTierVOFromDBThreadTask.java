package com.indiefund.market.domain.activity.service.trial.thread;

import com.indiefund.market.domain.activity.adapter.repository.IActivityRepository;
import com.indiefund.market.domain.activity.model.valobj.GameTierVO;

import java.util.concurrent.Callable;

/**
 * @description 查询商品信息任务
 * @create 2024-12-21 10:51
 */
public class QueryGameTierVOFromDBThreadTask implements Callable<GameTierVO> {

    private final String goodsId;

    private final IActivityRepository activityRepository;

    public QueryGameTierVOFromDBThreadTask(String goodsId, IActivityRepository activityRepository) {
        this.goodsId = goodsId;
        this.activityRepository = activityRepository;
    }

    @Override
    public GameTierVO call() throws Exception {
        return activityRepository.queryGameTierByGoodsId(goodsId);
    }

}
