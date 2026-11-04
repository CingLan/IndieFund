package com.indiefund.market.domain.trade.service;

import com.indiefund.market.domain.trade.model.entity.*;
import com.indiefund.market.domain.trade.model.valobj.GameCrowdfundingProgressVO;

/**
 * @description 众筹交易锁单服务接口
 * @create 2025-01-05 16:42
 */
public interface ITradeLockOrderService {

    /**
     * 查询，未被支付消费完成的营销优惠订单
     *
     * @param userId     用户ID
     * @param outTradeNo 外部唯一单号
     * @return 众筹，预购订单营销实体对象
     */
    MarketPayOrderEntity queryNoPayMarketPayOrderByOutTradeNo(String userId, String outTradeNo);

    /**
     * 查询众筹进度
     *
     * @param teamId 众筹ID
     * @return 进度
     */
    GameCrowdfundingProgressVO queryGameCrowdfundingProgress(String teamId);

    /**
     * 锁定，营销预支付订单；商品下单前，预购锁定。
     *
     * @param userEntity        用户根实体对象
     * @param payActivityEntity 众筹，支付活动实体对象
     * @param payDiscountEntity 众筹，支付优惠实体对象
     * @return 众筹，预购订单营销实体对象
     */
    MarketPayOrderEntity lockMarketPayOrder(UserEntity userEntity, PayActivityEntity payActivityEntity, PayDiscountEntity payDiscountEntity) throws Exception;

}
