package com.indiefund.market.domain.activity.service.trial.node;

import com.indiefund.market.domain.activity.model.entity.MarketProductEntity;
import com.indiefund.market.domain.activity.model.valobj.GameCrowdfundingActivityDiscountVO;
import com.indiefund.market.domain.activity.model.valobj.SCGameTierActivityVO;
import com.indiefund.market.domain.activity.model.valobj.GameTierVO;
import com.indiefund.market.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;

/**
 * @description 线程案例举例
 * @create 2025-04-03 07:44
 */
@Slf4j
//@Service
public class MarketNode2CompletableFuture extends MarketNode {

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    protected void multiThread(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
        // 异步查询活动配置
        CompletableFuture<GameCrowdfundingActivityDiscountVO> gameCrowdfundingActivityDiscountVOCompletableFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Long availableActivityId = requestParameter.getActivityId();
                if (null == requestParameter.getActivityId()) {
                    // 查询渠道商品活动配置关联配置
                    SCGameTierActivityVO scGameTierActivityVO = repository.querySCGameTierActivityBySCGoodsId(requestParameter.getSource(), requestParameter.getChannel(), requestParameter.getGoodsId());
                    if (null == scGameTierActivityVO) return null;
                    availableActivityId = scGameTierActivityVO.getActivityId();
                }
                // 查询活动配置
                return repository.queryGameCrowdfundingActivityDiscountVO(availableActivityId);
            } catch (Exception e) {
                log.error("异步查询活动配置异常", e);
                return null;
            }
        }, threadPoolExecutor);

        // 异步查询商品信息 - 在实际生产中，商品有同步库或者调用接口查询。这里暂时使用DB方式查询。
        CompletableFuture<GameTierVO> gameTierVOCompletableFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return repository.queryGameTierByGoodsId(requestParameter.getGoodsId());
            } catch (Exception e) {
                log.error("异步查询商品信息异常", e);
                return null;
            }
        }, threadPoolExecutor);

        // 等待所有异步任务完成并写入上下文
        CompletableFuture.allOf(gameCrowdfundingActivityDiscountVOCompletableFuture, gameTierVOCompletableFuture)
                .thenRun(() -> {
                    dynamicContext.setGameCrowdfundingActivityDiscountVO(gameCrowdfundingActivityDiscountVOCompletableFuture.join());
                    dynamicContext.setGameTierVO(gameTierVOCompletableFuture.join());
                }).join();

        log.info("众筹商品查询试算服务-MarketNode userId:{} 异步线程加载数据「GroupBuyActivityDiscountVO、SkuVO」完成", requestParameter.getUserId());
    }
}
