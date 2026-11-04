package com.indiefund.market.api;

import com.indiefund.market.api.dto.LockMarketPayOrderRequestDTO;
import com.indiefund.market.api.dto.LockMarketPayOrderResponseDTO;
import com.indiefund.market.api.dto.RefundMarketPayOrderRequestDTO;
import com.indiefund.market.api.dto.RefundMarketPayOrderResponseDTO;
import com.indiefund.market.api.dto.SettlementMarketPayOrderRequestDTO;
import com.indiefund.market.api.dto.SettlementMarketPayOrderResponseDTO;
import com.indiefund.market.api.response.Response;

/**
 * @description 营销交易服务接口
 * @create 2025-01-11 13:49
 */
public interface IMarketTradeService {

    /**
     * 营销锁单
     *
     * @param requestDTO 锁单商品信息
     * @return 锁单结果信息
     */
    Response<LockMarketPayOrderResponseDTO> lockMarketPayOrder(LockMarketPayOrderRequestDTO requestDTO);

    /**
     * 营销结算
     *
     * @param requestDTO 结算商品信息
     * @return 结算结果信息
     */
    Response<SettlementMarketPayOrderResponseDTO> settlementMarketPayOrder(SettlementMarketPayOrderRequestDTO requestDTO);

    /**
     * 营销众筹退单
     *
     * @param requestDTO 退单请求信息
     * @return 退单结果信息
     */
    Response<RefundMarketPayOrderResponseDTO> refundMarketPayOrder(RefundMarketPayOrderRequestDTO requestDTO);

}
