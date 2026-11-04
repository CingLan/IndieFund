package com.indiefund.market.domain.trade.model.aggregate;

import com.indiefund.market.domain.trade.model.entity.GameCrowdfundingTeamEntity;
import com.indiefund.market.domain.trade.model.entity.TradePaySuccessEntity;
import com.indiefund.market.domain.trade.model.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description 众筹组队结算聚合
 * @create 2025-01-26 16:38
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameCrowdfundingTeamSettlementAggregate {

    /** 用户实体对象 */
    private UserEntity userEntity;
    /** 众筹组队实体对象 */
    private GameCrowdfundingTeamEntity gameCrowdfundingTeamEntity;
    /** 交易支付订单实体对象 */
    private TradePaySuccessEntity tradePaySuccessEntity;

}
