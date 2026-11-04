package com.indiefund.market.domain.trade.model.aggregate;

import com.indiefund.market.domain.trade.model.entity.TradeRefundOrderEntity;
import com.indiefund.market.domain.trade.model.valobj.GameCrowdfundingProgressVO;
import com.indiefund.market.types.enums.GameCrowdfundingOrderEnumVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 众筹退单聚合
 *
 * 2025/7/11 19:30
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameCrowdfundingRefundAggregate {

    /**
     * 交易退单
     */
    private TradeRefundOrderEntity tradeRefundOrderEntity;

    /**
     * 退单进度
     */
    private GameCrowdfundingProgressVO gameCrowdfundingProgress;

    /**
     * 众筹枚举
     */
    private GameCrowdfundingOrderEnumVO gameCrowdfundingOrderEnumVO;

    public static GameCrowdfundingRefundAggregate buildUnpaid2RefundAggregate(TradeRefundOrderEntity tradeRefundOrderEntity, Integer lockCount) {
        GameCrowdfundingRefundAggregate gameCrowdfundingRefundAggregate = new GameCrowdfundingRefundAggregate();
        gameCrowdfundingRefundAggregate.setTradeRefundOrderEntity(tradeRefundOrderEntity);
        gameCrowdfundingRefundAggregate.setGameCrowdfundingProgress(
                GameCrowdfundingProgressVO.builder()
                        .lockCount(lockCount)
                        .build());

        return gameCrowdfundingRefundAggregate;
    }

    public static GameCrowdfundingRefundAggregate buildPaid2RefundAggregate(TradeRefundOrderEntity tradeRefundOrderEntity,
                                                                      Integer lockCount,
                                                                      Integer completeCount) {
        GameCrowdfundingRefundAggregate gameCrowdfundingRefundAggregate = new GameCrowdfundingRefundAggregate();
        gameCrowdfundingRefundAggregate.setTradeRefundOrderEntity(tradeRefundOrderEntity);
        gameCrowdfundingRefundAggregate.setGameCrowdfundingProgress(
                GameCrowdfundingProgressVO.builder()
                        .lockCount(lockCount)
                        .completeCount(completeCount)
                        .build());

        return gameCrowdfundingRefundAggregate;
    }

    public static GameCrowdfundingRefundAggregate buildPaidTeam2RefundAggregate(TradeRefundOrderEntity tradeRefundOrderEntity,
                                                                    Integer lockCount,
                                                                    Integer completeCount,
                                                                    GameCrowdfundingOrderEnumVO gameCrowdfundingOrderEnumVO) {
        GameCrowdfundingRefundAggregate gameCrowdfundingRefundAggregate = new GameCrowdfundingRefundAggregate();
        gameCrowdfundingRefundAggregate.setTradeRefundOrderEntity(tradeRefundOrderEntity);
        gameCrowdfundingRefundAggregate.setGameCrowdfundingProgress(
                GameCrowdfundingProgressVO.builder()
                        .lockCount(lockCount)
                        .completeCount(completeCount)
                        .build());
        gameCrowdfundingRefundAggregate.setGameCrowdfundingOrderEnumVO(gameCrowdfundingOrderEnumVO);

        return gameCrowdfundingRefundAggregate;
    }

}
