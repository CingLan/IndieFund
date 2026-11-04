package com.indiefund.market.trigger.http;

import com.indiefund.market.api.IMarketIndexService;
import com.indiefund.market.api.dto.GoodsMarketRequestDTO;
import com.indiefund.market.api.dto.GoodsMarketResponseDTO;
import com.indiefund.market.api.response.Response;
import com.indiefund.market.domain.activity.model.entity.MarketProductEntity;
import com.indiefund.market.domain.activity.model.entity.TrialBalanceEntity;
import com.indiefund.market.domain.activity.model.entity.UserGameCrowdfundingOrderDetailEntity;
import com.indiefund.market.domain.activity.model.valobj.GameCrowdfundingActivityDiscountVO;
import com.indiefund.market.domain.activity.model.valobj.TeamStatisticVO;
import com.indiefund.market.domain.activity.service.IIndexGameCrowdfundingMarketService;
import com.indiefund.market.types.enums.ResponseCode;
import cn.bugstack.wrench.rate.limiter.types.annotations.RateLimiterAccessInterceptor;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @description 营销首页服务
 * @create 2025-02-02 16:03
 */
@Slf4j
@RestController()
@CrossOrigin("*")
@RequestMapping("/api/v1/icf/index/")
public class MarketIndexController implements IMarketIndexService {

    @Resource
    private IIndexGameCrowdfundingMarketService indexGameCrowdfundingMarketService;

    @RateLimiterAccessInterceptor(key = "userId", fallbackMethod = "queryGameCrowdfundingMarketConfigFallBack", permitsPerSecond = 1.0d, blacklistCount = 1)
    @RequestMapping(value = "query_group_buy_market_config", method = RequestMethod.POST)
    @Override
    public Response<GoodsMarketResponseDTO> queryGameCrowdfundingMarketConfig(@RequestBody GoodsMarketRequestDTO requestDTO) {
        try {
            log.info("查询众筹档位支持配置开始:{} goodsId:{}", requestDTO.getUserId(), requestDTO.getGoodsId());

            if (StringUtils.isBlank(requestDTO.getUserId()) || StringUtils.isBlank(requestDTO.getSource()) || StringUtils.isBlank(requestDTO.getChannel()) || StringUtils.isBlank(requestDTO.getGoodsId())) {
                return Response.<GoodsMarketResponseDTO>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info(ResponseCode.ILLEGAL_PARAMETER.getInfo())
                        .build();
            }

            // 1. 营销优惠试算
            TrialBalanceEntity trialBalanceEntity = indexGameCrowdfundingMarketService.indexMarketTrial(MarketProductEntity.builder()
                    .userId(requestDTO.getUserId())
                    .source(requestDTO.getSource())
                    .channel(requestDTO.getChannel())
                    .goodsId(requestDTO.getGoodsId())
                    .build());


            GameCrowdfundingActivityDiscountVO gameCrowdfundingActivityDiscountVO = trialBalanceEntity.getGameCrowdfundingActivityDiscountVO();
            Long activityId = gameCrowdfundingActivityDiscountVO.getActivityId();

            // 2. 查询众筹组队
            List<UserGameCrowdfundingOrderDetailEntity> userGameCrowdfundingOrderDetailEntities = indexGameCrowdfundingMarketService.queryInProgressUserGameCrowdfundingOrderDetailList(activityId, requestDTO.getUserId(), 1, 2);

            // 3. 统计众筹数据
            TeamStatisticVO teamStatisticVO = indexGameCrowdfundingMarketService.queryTeamStatisticByActivityId(activityId);

            GoodsMarketResponseDTO.Goods goods = GoodsMarketResponseDTO.Goods.builder()
                    .goodsId(trialBalanceEntity.getGoodsId())
                    .originalPrice(trialBalanceEntity.getOriginalPrice())
                    .deductionPrice(trialBalanceEntity.getDeductionPrice())
                    .payPrice(trialBalanceEntity.getPayPrice())
                    .build();

            List<GoodsMarketResponseDTO.Team> teams = new ArrayList<>();
            if (null != userGameCrowdfundingOrderDetailEntities && !userGameCrowdfundingOrderDetailEntities.isEmpty()) {
                for (UserGameCrowdfundingOrderDetailEntity userGameCrowdfundingOrderDetailEntity : userGameCrowdfundingOrderDetailEntities) {
                    GoodsMarketResponseDTO.Team team = GoodsMarketResponseDTO.Team.builder()
                            .userId(userGameCrowdfundingOrderDetailEntity.getUserId())
                            .teamId(userGameCrowdfundingOrderDetailEntity.getTeamId())
                            .activityId(userGameCrowdfundingOrderDetailEntity.getActivityId())
                            .targetCount(userGameCrowdfundingOrderDetailEntity.getTargetCount())
                            .completeCount(userGameCrowdfundingOrderDetailEntity.getCompleteCount())
                            .lockCount(userGameCrowdfundingOrderDetailEntity.getLockCount())
                            .validStartTime(userGameCrowdfundingOrderDetailEntity.getValidStartTime())
                            .validEndTime(userGameCrowdfundingOrderDetailEntity.getValidEndTime())
                            .validTimeCountdown(GoodsMarketResponseDTO.Team.differenceDateTime2Str(new Date(), userGameCrowdfundingOrderDetailEntity.getValidEndTime()))
                            .outTradeNo(userGameCrowdfundingOrderDetailEntity.getOutTradeNo())
                            .build();
                    teams.add(team);
                }
            }

            GoodsMarketResponseDTO.TeamStatistic teamStatistic = GoodsMarketResponseDTO.TeamStatistic.builder()
                    .allTeamCount(teamStatisticVO.getAllTeamCount())
                    .allTeamCompleteCount(teamStatisticVO.getAllTeamCompleteCount())
                    .allTeamUserCount(teamStatisticVO.getAllTeamUserCount())
                    .build();

            Response<GoodsMarketResponseDTO> response = Response.<GoodsMarketResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(GoodsMarketResponseDTO.builder()
                            .activityId(activityId)
                            .goods(goods)
                            .teamList(teams)
                            .teamStatistic(teamStatistic)
                            .build())
                    .build();

            log.info("查询众筹档位支持配置完成:{} goodsId:{} response:{}", requestDTO.getUserId(), requestDTO.getGoodsId(), JSON.toJSONString(response));

            return response;
        } catch (Exception e) {
            log.error("查询众筹档位支持配置失败:{} goodsId:{}", requestDTO.getUserId(), requestDTO.getGoodsId(), e);
            return Response.<GoodsMarketResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    public Response<GoodsMarketResponseDTO> queryGameCrowdfundingMarketConfigFallBack(@RequestBody GoodsMarketRequestDTO requestDTO) {
        log.error("查询众筹档位支持配置限流:{}", requestDTO.getUserId());
        return Response.<GoodsMarketResponseDTO>builder()
                .code(ResponseCode.RATE_LIMITER.getCode())
                .info(ResponseCode.RATE_LIMITER.getInfo())
                .build();
    }

}
