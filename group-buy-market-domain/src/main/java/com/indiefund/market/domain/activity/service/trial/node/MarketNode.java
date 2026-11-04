package com.indiefund.market.domain.activity.service.trial.node;

import com.indiefund.market.domain.activity.model.entity.MarketProductEntity;
import com.indiefund.market.domain.activity.model.entity.TrialBalanceEntity;
import com.indiefund.market.domain.activity.model.valobj.GameCrowdfundingActivityDiscountVO;
import com.indiefund.market.domain.activity.model.valobj.SCGameTierActivityVO;
import com.indiefund.market.domain.activity.model.valobj.GameTierVO;
import com.indiefund.market.domain.activity.service.discount.IDiscountCalculateService;
import com.indiefund.market.domain.activity.service.trial.AbstractGameCrowdfundingMarketSupport;
import com.indiefund.market.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import com.indiefund.market.domain.activity.service.trial.thread.QueryGameCrowdfundingActivityDiscountVOThreadTask;
import com.indiefund.market.domain.activity.service.trial.thread.QueryGameTierVOFromDBThreadTask;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.indiefund.market.types.enums.ResponseCode;
import com.indiefund.market.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * @description 营销优惠节点
 * @create 2024-12-14 14:30
 */
@Slf4j
@Service
public class MarketNode extends AbstractGameCrowdfundingMarketSupport<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    @Resource
    private Map<String, IDiscountCalculateService> discountCalculateServiceMap;
    @Resource
    private ErrorNode errorNode;
    @Resource
    private TagNode tagNode;

    /**
     * 在 MarketNode2CompletableFuture 继承的子类实现一个 CompletableFuture 多线程方式。
     * <p>
     * 1. CompletableFuture：适用于大多数现代 Java 应用，尤其在需要灵活任务编排时。
     * 2.  FutureTask：任务极度简单，适合简单场景。
     * <p>
     * | 对比维度    | FutureTask             | CompletableFuture                |
     * | :--------------- | :-------------------------- | :------------------------------------- |
     * | 任务编排能力 | 弱（需手动管理多个 Future） | 强（内置 `thenApply`、`allOf` 等方法） |
     * | 代码简洁性  | 冗余（显式调用 `get()`）    | 简洁（链式调用，逻辑内聚）             |
     * | 异常处理   | 繁琐（需捕获多个异常）      | 优雅（支持 `exceptionally` 统一处理）  |
     * | 线程阻塞     | 可能多次阻塞主线程          | 非阻塞或单次阻塞（如 `join()`）        |
     * | 适用场景     | 简单任务、低版本 Java 环境  | 复杂异步流程、Java 8+ 环境             |
     * <p>
     * 使用；MarketNode 的 @Service 注释掉，MarketNode2CompletableFuture 的 @Service 打开，就可以使用了。
     */
    @Override
    protected void multiThread(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
        // 异步查询活动配置
        QueryGameCrowdfundingActivityDiscountVOThreadTask queryGameCrowdfundingActivityDiscountVOThreadTask = new QueryGameCrowdfundingActivityDiscountVOThreadTask(requestParameter.getActivityId(), requestParameter.getSource(), requestParameter.getChannel(), requestParameter.getGoodsId(), repository);
        FutureTask<GameCrowdfundingActivityDiscountVO> gameCrowdfundingActivityDiscountVOFutureTask = new FutureTask<>(queryGameCrowdfundingActivityDiscountVOThreadTask);
        threadPoolExecutor.execute(gameCrowdfundingActivityDiscountVOFutureTask);

        // 异步查询商品信息 - 在实际生产中，商品有同步库或者调用接口查询。这里暂时使用DB方式查询。
        QueryGameTierVOFromDBThreadTask queryGameTierVOFromDBThreadTask = new QueryGameTierVOFromDBThreadTask(requestParameter.getGoodsId(), repository);
        FutureTask<GameTierVO> gameTierVOFutureTask = new FutureTask<>(queryGameTierVOFromDBThreadTask);
        threadPoolExecutor.execute(gameTierVOFutureTask);

        // 写入上下文 - 对于一些复杂场景，获取数据的操作，有时候会在下N个节点获取，这样前置查询数据，可以提高接口响应效率
        dynamicContext.setGameCrowdfundingActivityDiscountVO(gameCrowdfundingActivityDiscountVOFutureTask.get(timeout, TimeUnit.MILLISECONDS));
        dynamicContext.setGameTierVO(gameTierVOFutureTask.get(timeout, TimeUnit.MILLISECONDS));

        log.info("众筹商品查询试算服务-MarketNode userId:{} 异步线程加载数据「GroupBuyActivityDiscountVO、SkuVO」完成", requestParameter.getUserId());
    }

    @Override
    public TrialBalanceEntity doApply(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("众筹商品查询试算服务-MarketNode userId:{} requestParameter:{}", requestParameter.getUserId(), JSON.toJSONString(requestParameter));

        // 获取上下文数据
        GameCrowdfundingActivityDiscountVO gameCrowdfundingActivityDiscountVO = dynamicContext.getGameCrowdfundingActivityDiscountVO();
        if (null == gameCrowdfundingActivityDiscountVO) {
            return router(requestParameter, dynamicContext);
        }

        GameCrowdfundingActivityDiscountVO.GameCrowdfundingDiscount gameCrowdfundingDiscount = gameCrowdfundingActivityDiscountVO.getGameCrowdfundingDiscount();
        GameTierVO gameTierVO = dynamicContext.getGameTierVO();
        if (null == gameCrowdfundingDiscount || null == gameTierVO) {
            return router(requestParameter, dynamicContext);
        }

        // 优惠试算
        IDiscountCalculateService discountCalculateService = discountCalculateServiceMap.get(gameCrowdfundingDiscount.getMarketPlan());
        if (null == discountCalculateService) {
            log.info("不存在{}类型的折扣计算服务，支持类型为:{}", gameCrowdfundingDiscount.getMarketPlan(), JSON.toJSONString(discountCalculateServiceMap.keySet()));
            throw new AppException(ResponseCode.E0001.getCode(), ResponseCode.E0001.getInfo());
        }

        // 折扣价格
        BigDecimal payPrice = discountCalculateService.calculate(requestParameter.getUserId(), gameTierVO.getOriginalPrice(), gameCrowdfundingDiscount);
        dynamicContext.setDeductionPrice(gameTierVO.getOriginalPrice().subtract(payPrice));
        dynamicContext.setPayPrice(payPrice);

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        // 不存在配置的众筹活动，走异常节点
        if (null == dynamicContext.getGameCrowdfundingActivityDiscountVO() || null == dynamicContext.getGameTierVO() || null == dynamicContext.getDeductionPrice()) {
            return errorNode;
        }

        return tagNode;
    }

}
