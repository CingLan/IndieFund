package com.indiefund.market.domain.trade.service.lock.filter;

import com.indiefund.market.domain.trade.adapter.repository.ITradeRepository;
import com.indiefund.market.domain.trade.model.entity.GameCrowdfundingActivityEntity;
import com.indiefund.market.domain.trade.model.entity.TradeLockRuleCommandEntity;
import com.indiefund.market.domain.trade.model.entity.TradeLockRuleFilterBackEntity;
import com.indiefund.market.domain.trade.service.lock.factory.TradeLockRuleFilterFactory;
import com.indiefund.market.types.enums.ActivityStatusEnumVO;
import com.indiefund.market.types.enums.ResponseCode;
import com.indiefund.market.types.exception.AppException;
import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @description 活动的可用性，规则过滤【状态、有效期】
 * @create 2025-01-25 09:18
 */
@Slf4j
@Service
public class ActivityUsabilityRuleFilter implements ILogicHandler<TradeLockRuleCommandEntity, TradeLockRuleFilterFactory.DynamicContext, TradeLockRuleFilterBackEntity> {

    @Resource
    private ITradeRepository repository;

    @Override
    public TradeLockRuleFilterBackEntity apply(TradeLockRuleCommandEntity requestParameter, TradeLockRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("交易规则过滤-活动的可用性校验{} activityId:{}", requestParameter.getUserId(), requestParameter.getActivityId());

        // 查询众筹活动
        GameCrowdfundingActivityEntity gameCrowdfundingActivity = repository.queryGameCrowdfundingActivityEntityByActivityId(requestParameter.getActivityId());

        // 校验；活动状态 - 可以抛业务异常code，或者把code写入到动态上下文dynamicContext中，最后获取。
        if (!ActivityStatusEnumVO.EFFECTIVE.equals(gameCrowdfundingActivity.getStatus())) {
            log.info("活动的可用性校验，非生效状态 activityId:{}", requestParameter.getActivityId());
            throw new AppException(ResponseCode.E0101);
        }

        // 校验；活动时间
        Date currentTime = new Date();
        if (currentTime.before(gameCrowdfundingActivity.getStartTime()) || currentTime.after(gameCrowdfundingActivity.getEndTime())) {
            log.info("活动的可用性校验，非可参与时间范围 activityId:{}", requestParameter.getActivityId());
            throw new AppException(ResponseCode.E0102);
        }

        // 写入动态上下文
        dynamicContext.setGameCrowdfundingActivity(gameCrowdfundingActivity);

        // 走到下一个责任链节点
        return next(requestParameter, dynamicContext);
    }

}
