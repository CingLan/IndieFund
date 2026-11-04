package com.indiefund.market.domain.trade.adapter.port;

import com.indiefund.market.domain.trade.model.entity.NotifyTaskEntity;

/**
 * @description 交易接口服务接口
 * @create 2025-01-31 10:38
 */
public interface ITradePort {

    String gameCrowdfundingNotify(NotifyTaskEntity notifyTask) throws Exception;

}
