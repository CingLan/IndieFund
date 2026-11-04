package com.indiefund.market.domain.activity.service;

import com.indiefund.market.domain.activity.adapter.repository.IActivityRepository;
import com.indiefund.market.domain.activity.model.entity.UserGameCrowdfundingOrderDetailEntity;
import com.indiefund.market.domain.activity.model.entity.MarketProductEntity;
import com.indiefund.market.domain.activity.model.entity.TrialBalanceEntity;
import com.indiefund.market.domain.activity.model.valobj.TeamStatisticVO;
import com.indiefund.market.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @description 首页营销服务
 * @create 2024-12-14 14:33
 */
@Service
public class IndexGameCrowdfundingMarketServiceImpl implements IIndexGameCrowdfundingMarketService {

    @Resource
    private DefaultActivityStrategyFactory defaultActivityStrategyFactory;
    @Resource
    private IActivityRepository repository;

    @Override
    public TrialBalanceEntity indexMarketTrial(MarketProductEntity marketProductEntity) throws Exception {
        // 获取执行策略
        StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> strategyHandler = defaultActivityStrategyFactory.strategyHandler();
        // 受理试算操作
        return strategyHandler.apply(marketProductEntity, new DefaultActivityStrategyFactory.DynamicContext());
    }

    @Override
    public List<UserGameCrowdfundingOrderDetailEntity> queryInProgressUserGameCrowdfundingOrderDetailList(Long activityId, String userId, Integer ownerCount, Integer randomCount) {
        List<UserGameCrowdfundingOrderDetailEntity> unionAllList = new ArrayList<>();

        // 查询个人众筹数据
        if (0 != ownerCount) {
            List<UserGameCrowdfundingOrderDetailEntity> ownerList = repository.queryInProgressUserGameCrowdfundingOrderDetailListByOwner(activityId, userId, ownerCount);
            if (null != ownerList && !ownerList.isEmpty()){
                unionAllList.addAll(ownerList);
            }
        }

        // 查询其他非个人众筹
        if (0 != randomCount) {
            List<UserGameCrowdfundingOrderDetailEntity> randomList = repository.queryInProgressUserGameCrowdfundingOrderDetailListByRandom(activityId, userId, randomCount);
            if (null != randomList && !randomList.isEmpty()){
                unionAllList.addAll(randomList);
            }
        }

        return unionAllList;
    }

    @Override
    public TeamStatisticVO queryTeamStatisticByActivityId(Long activityId) {
        return repository.queryTeamStatisticByActivityId(activityId);
    }

}
