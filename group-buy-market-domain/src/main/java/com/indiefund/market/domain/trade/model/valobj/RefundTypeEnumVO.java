package com.indiefund.market.domain.trade.model.valobj;

import com.indiefund.market.domain.trade.model.entity.TradeRefundOrderEntity;
import com.indiefund.market.types.enums.GameCrowdfundingOrderEnumVO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;

/**
 * 退单类型枚举
 *
 * 2025/7/11 18:59
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum RefundTypeEnumVO {

    UNPAID_UNLOCK("unpaid_unlock", "unpaid2RefundStrategy", "未支付，未成团") {
        @Override
        public boolean matches(GameCrowdfundingOrderEnumVO gameCrowdfundingOrderEnumVO, TradeOrderStatusEnumVO tradeOrderStatusEnumVO) {
            return GameCrowdfundingOrderEnumVO.PROGRESS.equals(gameCrowdfundingOrderEnumVO) && TradeOrderStatusEnumVO.CREATE.equals(tradeOrderStatusEnumVO);
        }
    },
    
    PAID_UNFORMED("paid_unformed", "paid2RefundStrategy", "已支付，未成团") {
        @Override
        public boolean matches(GameCrowdfundingOrderEnumVO gameCrowdfundingOrderEnumVO, TradeOrderStatusEnumVO tradeOrderStatusEnumVO) {
            return GameCrowdfundingOrderEnumVO.PROGRESS.equals(gameCrowdfundingOrderEnumVO) && TradeOrderStatusEnumVO.COMPLETE.equals(tradeOrderStatusEnumVO);
        }
    },
    
    PAID_FORMED("paid_formed", "paidTeam2RefundStrategy", "已支付，已成团") {
        @Override
        public boolean matches(GameCrowdfundingOrderEnumVO gameCrowdfundingOrderEnumVO, TradeOrderStatusEnumVO tradeOrderStatusEnumVO) {
            // 完成、完成含退单，都做此处理
            return (GameCrowdfundingOrderEnumVO.COMPLETE.equals(gameCrowdfundingOrderEnumVO) || GameCrowdfundingOrderEnumVO.COMPLETE_FAIL.equals(gameCrowdfundingOrderEnumVO))
                    && TradeOrderStatusEnumVO.COMPLETE.equals(tradeOrderStatusEnumVO);
        }
    },
    ;

    private String code;
    private String strategy;
    private String info;

    /**
     * 抽象方法，由每个枚举值实现自己的匹配逻辑
     */
    public abstract boolean matches(GameCrowdfundingOrderEnumVO gameCrowdfundingOrderEnumVO, TradeOrderStatusEnumVO tradeOrderStatusEnumVO);

    /**
     * 根据状态组合获取对应的退款策略枚举
     */
    public static RefundTypeEnumVO getRefundStrategy(GameCrowdfundingOrderEnumVO gameCrowdfundingOrderEnumVO, TradeOrderStatusEnumVO tradeOrderStatusEnumVO) {
        return Arrays.stream(values())
                .filter(refundType -> refundType.matches(gameCrowdfundingOrderEnumVO, tradeOrderStatusEnumVO))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("不支持的退款状态组合: groupBuyOrderStatus=" + gameCrowdfundingOrderEnumVO + ", tradeOrderStatus=" + tradeOrderStatusEnumVO));
    }

    public static RefundTypeEnumVO getRefundTypeEnumVOByCode(String code) {
        switch (code) {
            case "unpaid_unlock":
                return UNPAID_UNLOCK;
            case "paid_unformed":
                return PAID_UNFORMED;
            case "paid_formed":
                return PAID_FORMED;
        }
        throw new RuntimeException("退单类型枚举值不存在: " + code);
    }

}
