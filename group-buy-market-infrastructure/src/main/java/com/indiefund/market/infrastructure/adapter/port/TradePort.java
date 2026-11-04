package com.indiefund.market.infrastructure.adapter.port;

import com.indiefund.market.domain.trade.adapter.port.ITradePort;
import com.indiefund.market.domain.trade.model.entity.NotifyTaskEntity;
import com.indiefund.market.domain.trade.model.valobj.NotifyTypeEnumVO;
import com.indiefund.market.infrastructure.event.EventPublisher;
import com.indiefund.market.infrastructure.gateway.GameCrowdfundingNotifyService;
import com.indiefund.market.infrastructure.redis.IRedisService;
import com.indiefund.market.types.enums.NotifyTaskHTTPEnumVO;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @description 交易接口服务
 * @create 2025-01-31 13:34
 */
@Service
public class TradePort implements ITradePort {

    @Resource
    private GameCrowdfundingNotifyService gameCrowdfundingNotifyService;
    @Resource
    private IRedisService redisService;
    @Resource
    private EventPublisher publisher;

    @Override
    public String gameCrowdfundingNotify(NotifyTaskEntity notifyTask) throws Exception {
        RLock lock = redisService.getLock(notifyTask.lockKey());
        try {
            // group-buy-market 众筹服务端会被部署到多台应用服务器上，那么就会有很多任务一起执行。这个时候要进行抢占，避免被多次执行
            if (lock.tryLock(3, 0, TimeUnit.SECONDS)) {
                try {
                    // 回调方式 HTTP
                    if (NotifyTypeEnumVO.HTTP.getCode().equals(notifyTask.getNotifyType())) {
                        // 无效的 notifyUrl 则直接返回成功
                        if (StringUtils.isBlank(notifyTask.getNotifyUrl()) || "暂无".equals(notifyTask.getNotifyUrl())) {
                            return NotifyTaskHTTPEnumVO.SUCCESS.getCode();
                        }
                        gameCrowdfundingNotifyService.gameCrowdfundingNotify(notifyTask.getNotifyUrl(), notifyTask.getParameterJson());
                        return NotifyTaskHTTPEnumVO.SUCCESS.getCode();
                    }

                    // 回调方式 MQ
                    if (NotifyTypeEnumVO.MQ.getCode().equals(notifyTask.getNotifyType())) {
                        publisher.publish(notifyTask.getNotifyMQ(), notifyTask.getParameterJson());
                        return NotifyTaskHTTPEnumVO.SUCCESS.getCode();
                    }
                } finally {
                    if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            }
            return NotifyTaskHTTPEnumVO.NULL.getCode();
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            return NotifyTaskHTTPEnumVO.ERROR.getCode();
        }
    }

}
