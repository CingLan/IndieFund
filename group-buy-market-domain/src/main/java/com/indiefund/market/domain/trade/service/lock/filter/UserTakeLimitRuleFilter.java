package com.indiefund.market.domain.trade.service.lock.filter;

import com.indiefund.market.domain.trade.adapter.repository.ITradeRepository;
import com.indiefund.market.domain.trade.model.entity.GameCrowdfundingActivityEntity;
import com.indiefund.market.domain.trade.model.entity.TradeLockRuleCommandEntity;
import com.indiefund.market.domain.trade.model.entity.TradeLockRuleFilterBackEntity;
import com.indiefund.market.domain.trade.service.lock.factory.TradeLockRuleFilterFactory;
import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import com.indiefund.market.types.enums.ResponseCode;
import com.indiefund.market.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @description 用户参与限制，规则过滤
 * @create 2025-01-25 09:19
 */
@Slf4j
@Service
public class UserTakeLimitRuleFilter implements ILogicHandler<TradeLockRuleCommandEntity, TradeLockRuleFilterFactory.DynamicContext, TradeLockRuleFilterBackEntity> {

    @Resource
    private ITradeRepository repository;

    @Override
    public TradeLockRuleFilterBackEntity apply(TradeLockRuleCommandEntity requestParameter, TradeLockRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("交易规则过滤-用户参与次数校验{} activityId:{}", requestParameter.getUserId(), requestParameter.getActivityId());

        GameCrowdfundingActivityEntity gameCrowdfundingActivity = dynamicContext.getGameCrowdfundingActivity();

        // 查询用户在一个众筹活动上参与的次数
        Integer count = repository.queryOrderCountByActivityId(requestParameter.getActivityId(), requestParameter.getUserId());

        if (null != gameCrowdfundingActivity.getTakeLimitCount() && count >= gameCrowdfundingActivity.getTakeLimitCount()) {
            log.info("用户参与次数校验，已达可参与上限 activityId:{}", requestParameter.getActivityId());
            throw new AppException(ResponseCode.E0103);
        }

        dynamicContext.setUserTakeOrderCount(count);

        // 走到下一个责任链节点
        return next(requestParameter, dynamicContext);
    }

}
