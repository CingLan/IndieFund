package com.indiefund.market.infrastructure.adapter.repository;

import com.indiefund.market.domain.activity.model.entity.UserGameCrowdfundingOrderDetailEntity;
import com.indiefund.market.domain.trade.adapter.repository.ITradeRepository;
import com.indiefund.market.domain.trade.model.aggregate.GameCrowdfundingOrderAggregate;
import com.indiefund.market.domain.trade.model.aggregate.GameCrowdfundingRefundAggregate;
import com.indiefund.market.domain.trade.model.aggregate.GameCrowdfundingTeamSettlementAggregate;
import com.indiefund.market.domain.trade.model.entity.*;
import com.indiefund.market.domain.trade.model.valobj.*;
import com.indiefund.market.infrastructure.dao.IGameCrowdfundingActivityDao;
import com.indiefund.market.infrastructure.dao.IGameCrowdfundingOrderDao;
import com.indiefund.market.infrastructure.dao.IGameCrowdfundingOrderListDao;
import com.indiefund.market.infrastructure.dao.INotifyTaskDao;
import com.indiefund.market.infrastructure.dao.po.GameCrowdfundingActivity;
import com.indiefund.market.infrastructure.dao.po.GameCrowdfundingOrder;
import com.indiefund.market.infrastructure.dao.po.GameCrowdfundingOrderList;
import com.indiefund.market.infrastructure.dao.po.NotifyTask;
import com.indiefund.market.infrastructure.dcc.DCCService;
import com.indiefund.market.infrastructure.redis.IRedisService;
import com.indiefund.market.types.common.Constants;
import com.indiefund.market.types.enums.ActivityStatusEnumVO;
import com.indiefund.market.types.enums.GameCrowdfundingOrderEnumVO;
import com.indiefund.market.types.enums.ResponseCode;
import com.indiefund.market.types.exception.AppException;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @description 交易仓储服务
 * @create 2025-01-11 09:17
 */
@Slf4j
@Repository
public class TradeRepository implements ITradeRepository {

    @Resource
    private IGameCrowdfundingActivityDao gameCrowdfundingActivityDao;
    @Resource
    private IGameCrowdfundingOrderDao gameCrowdfundingOrderDao;
    @Resource
    private IGameCrowdfundingOrderListDao gameCrowdfundingOrderListDao;
    @Resource
    private INotifyTaskDao notifyTaskDao;
    @Resource
    private DCCService dccService;

    @Value("${spring.rabbitmq.config.producer.topic_team_success.routing_key}")
    private String topic_team_success;

    @Value("${spring.rabbitmq.config.producer.topic_team_refund.routing_key}")
    private String topic_team_refund;

    @Resource
    private IRedisService redisService;

    @Override
    public MarketPayOrderEntity queryMarketPayOrderEntityByOutTradeNo(String userId, String outTradeNo) {
        GameCrowdfundingOrderList gameCrowdfundingOrderListReq = new GameCrowdfundingOrderList();
        gameCrowdfundingOrderListReq.setUserId(userId);
        gameCrowdfundingOrderListReq.setOutTradeNo(outTradeNo);
        GameCrowdfundingOrderList gameCrowdfundingOrderListRes = gameCrowdfundingOrderListDao.queryGameCrowdfundingOrderRecordByOutTradeNo(gameCrowdfundingOrderListReq);
        if (null == gameCrowdfundingOrderListRes) return null;

        return MarketPayOrderEntity.builder()
                .teamId(gameCrowdfundingOrderListRes.getTeamId())
                .orderId(gameCrowdfundingOrderListRes.getOrderId())
                .originalPrice(gameCrowdfundingOrderListRes.getOriginalPrice())
                .deductionPrice(gameCrowdfundingOrderListRes.getDeductionPrice())
                .payPrice(gameCrowdfundingOrderListRes.getPayPrice())
                .tradeOrderStatusEnumVO(TradeOrderStatusEnumVO.valueOf(gameCrowdfundingOrderListRes.getStatus()))
                .build();
    }

    @Transactional(timeout = 500)
    @Override
    public MarketPayOrderEntity lockMarketPayOrder(GameCrowdfundingOrderAggregate gameCrowdfundingOrderAggregate) {
        // 聚合对象信息
        UserEntity userEntity = gameCrowdfundingOrderAggregate.getUserEntity();
        PayActivityEntity payActivityEntity = gameCrowdfundingOrderAggregate.getPayActivityEntity();
        PayDiscountEntity payDiscountEntity = gameCrowdfundingOrderAggregate.getPayDiscountEntity();
        NotifyConfigVO notifyConfigVO = payDiscountEntity.getNotifyConfigVO();
        Integer userTakeOrderCount = gameCrowdfundingOrderAggregate.getUserTakeOrderCount();

        // 判断是否有团 - teamId 为空 - 新团、为不空 - 老团
        String teamId = payActivityEntity.getTeamId();
        if (StringUtils.isBlank(teamId)) {
            // 使用 RandomStringUtils.randomNumeric 替代公司里使用的雪花算法UUID
            teamId = RandomStringUtils.randomNumeric(8);

            // 构建众筹订单
            GameCrowdfundingOrder gameCrowdfundingOrder = GameCrowdfundingOrder.builder()
                    .teamId(teamId)
                    .activityId(payActivityEntity.getActivityId())
                    .source(payDiscountEntity.getSource())
                    .channel(payDiscountEntity.getChannel())
                    .originalPrice(payDiscountEntity.getOriginalPrice())
                    .deductionPrice(payDiscountEntity.getDeductionPrice())
                    .payPrice(payDiscountEntity.getPayPrice())
                    .targetCount(payActivityEntity.getTargetCount())
                    .completeCount(0)
                    .lockCount(1)
                    .validStartTime(payActivityEntity.getStartTime())
                    .validEndTime(payActivityEntity.getEndTime())
                    .notifyType(notifyConfigVO.getNotifyType().getCode())
                    .notifyUrl(notifyConfigVO.getNotifyUrl())
                    .build();

            // 写入记录
            gameCrowdfundingOrderDao.insert(gameCrowdfundingOrder);
        } else {
            // 更新记录 - 如果更新记录不等于1，则表示众筹已满，抛出异常
            int updateAddTargetCount = gameCrowdfundingOrderDao.updateAddLockCount(teamId);
            if (1 != updateAddTargetCount) {
                throw new AppException(ResponseCode.E0005);
            }
        }

        // 日期处理
        Date currentDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        calendar.add(Calendar.MINUTE, payActivityEntity.getValidTime());

        // 使用 RandomStringUtils.randomNumeric 替代公司里使用的雪花算法UUID
        String orderId = RandomStringUtils.randomNumeric(12);
        GameCrowdfundingOrderList gameCrowdfundingOrderListReq = GameCrowdfundingOrderList.builder()
                .userId(userEntity.getUserId())
                .teamId(teamId)
                .orderId(orderId)
                .activityId(payActivityEntity.getActivityId())
                .startTime(currentDate)
                .endTime(calendar.getTime())
                .goodsId(payDiscountEntity.getGoodsId())
                .source(payDiscountEntity.getSource())
                .channel(payDiscountEntity.getChannel())
                .originalPrice(payDiscountEntity.getOriginalPrice())
                .deductionPrice(payDiscountEntity.getDeductionPrice())
                .payPrice(payDiscountEntity.getPayPrice())
                .status(TradeOrderStatusEnumVO.CREATE.getCode())
                .outTradeNo(payDiscountEntity.getOutTradeNo())
                // 构建 bizId 唯一值；活动id_用户id_参与次数累加
                .bizId(payActivityEntity.getActivityId() + Constants.UNDERLINE + userEntity.getUserId() + Constants.UNDERLINE + (userTakeOrderCount + 1))
                .build();
        try {
            // 写入众筹记录
            gameCrowdfundingOrderListDao.insert(gameCrowdfundingOrderListReq);
        } catch (DuplicateKeyException e) {
            throw new AppException(ResponseCode.INDEX_EXCEPTION);
        }

        return MarketPayOrderEntity.builder()
                .orderId(orderId)
                .originalPrice(payDiscountEntity.getOriginalPrice())
                .deductionPrice(payDiscountEntity.getDeductionPrice())
                .payPrice(payDiscountEntity.getPayPrice())
                .tradeOrderStatusEnumVO(TradeOrderStatusEnumVO.CREATE)
                .teamId(teamId)
                .build();
    }

    @Override
    public GameCrowdfundingProgressVO queryGameCrowdfundingProgress(String teamId) {
        GameCrowdfundingOrder gameCrowdfundingOrder = gameCrowdfundingOrderDao.queryGameCrowdfundingProgress(teamId);
        if (null == gameCrowdfundingOrder) return null;
        return GameCrowdfundingProgressVO.builder()
                .completeCount(gameCrowdfundingOrder.getCompleteCount())
                .targetCount(gameCrowdfundingOrder.getTargetCount())
                .lockCount(gameCrowdfundingOrder.getLockCount())
                .build();
    }

    @Override
    public GameCrowdfundingActivityEntity queryGameCrowdfundingActivityEntityByActivityId(Long activityId) {
        GameCrowdfundingActivity gameCrowdfundingActivity = gameCrowdfundingActivityDao.queryGameCrowdfundingActivityByActivityId(activityId);
        return GameCrowdfundingActivityEntity.builder()
                .activityId(gameCrowdfundingActivity.getActivityId())
                .activityName(gameCrowdfundingActivity.getActivityName())
                .discountId(gameCrowdfundingActivity.getDiscountId())
                .groupType(gameCrowdfundingActivity.getGroupType())
                .takeLimitCount(gameCrowdfundingActivity.getTakeLimitCount())
                .target(gameCrowdfundingActivity.getTarget())
                .validTime(gameCrowdfundingActivity.getValidTime())
                .status(ActivityStatusEnumVO.valueOf(gameCrowdfundingActivity.getStatus()))
                .startTime(gameCrowdfundingActivity.getStartTime())
                .endTime(gameCrowdfundingActivity.getEndTime())
                .tagId(gameCrowdfundingActivity.getTagId())
                .tagScope(gameCrowdfundingActivity.getTagScope())
                .build();
    }

    @Override
    public Integer queryOrderCountByActivityId(Long activityId, String userId) {
        GameCrowdfundingOrderList gameCrowdfundingOrderListReq = new GameCrowdfundingOrderList();
        gameCrowdfundingOrderListReq.setActivityId(activityId);
        gameCrowdfundingOrderListReq.setUserId(userId);
        return gameCrowdfundingOrderListDao.queryOrderCountByActivityId(gameCrowdfundingOrderListReq);
    }

    @Override
    public GameCrowdfundingTeamEntity queryGameCrowdfundingTeamByTeamId(String teamId) {
        GameCrowdfundingOrder gameCrowdfundingOrder = gameCrowdfundingOrderDao.queryGameCrowdfundingTeamByTeamId(teamId);
        return GameCrowdfundingTeamEntity.builder()
                .teamId(gameCrowdfundingOrder.getTeamId())
                .activityId(gameCrowdfundingOrder.getActivityId())
                .targetCount(gameCrowdfundingOrder.getTargetCount())
                .completeCount(gameCrowdfundingOrder.getCompleteCount())
                .lockCount(gameCrowdfundingOrder.getLockCount())
                .status(GameCrowdfundingOrderEnumVO.valueOf(gameCrowdfundingOrder.getStatus()))
                .validStartTime(gameCrowdfundingOrder.getValidStartTime())
                .validEndTime(gameCrowdfundingOrder.getValidEndTime())
                .notifyConfigVO(NotifyConfigVO.builder()
                        .notifyType(NotifyTypeEnumVO.valueOf(gameCrowdfundingOrder.getNotifyType()))
                        .notifyUrl(gameCrowdfundingOrder.getNotifyUrl())
                        // MQ 是固定的
                        .notifyMQ(topic_team_success)
                        .build())
                .build();
    }

    @Transactional(timeout = 5000)
    @Override
    public NotifyTaskEntity settlementMarketPayOrder(GameCrowdfundingTeamSettlementAggregate gameCrowdfundingTeamSettlementAggregate) {

        UserEntity userEntity = gameCrowdfundingTeamSettlementAggregate.getUserEntity();
        GameCrowdfundingTeamEntity gameCrowdfundingTeamEntity = gameCrowdfundingTeamSettlementAggregate.getGameCrowdfundingTeamEntity();
        NotifyConfigVO notifyConfigVO = gameCrowdfundingTeamEntity.getNotifyConfigVO();
        TradePaySuccessEntity tradePaySuccessEntity = gameCrowdfundingTeamSettlementAggregate.getTradePaySuccessEntity();

        // 1. 更新众筹订单明细状态
        GameCrowdfundingOrderList gameCrowdfundingOrderListReq = new GameCrowdfundingOrderList();
        gameCrowdfundingOrderListReq.setUserId(userEntity.getUserId());
        gameCrowdfundingOrderListReq.setOutTradeNo(tradePaySuccessEntity.getOutTradeNo());
        gameCrowdfundingOrderListReq.setOutTradeTime(tradePaySuccessEntity.getOutTradeTime());

        int updateOrderListStatusCount = gameCrowdfundingOrderListDao.updateOrderStatus2COMPLETE(gameCrowdfundingOrderListReq);
        if (1 != updateOrderListStatusCount) {
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        // 2. 更新众筹达成数量
        int updateAddCount = gameCrowdfundingOrderDao.updateAddCompleteCount(gameCrowdfundingTeamEntity.getTeamId());
        if (1 != updateAddCount) {
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        // 3. 更新众筹完成状态
        // 【面试题，这个地方可能会有一个并发情况，就是多个用户拿到的 gameCrowdfundingTeamEntity.getCompleteCount() 是同一个值怎么办？】
        // 【方式1；可以给调用 settlementMarketPayOrder 结算方法的地方，添加一个分布式锁，让结算只能顺序执行】
        // 【方式2；这部分结算，只做数据库的更新操作，以及发送mq，之后在消费mq的地方，做结算。】
        // 【方式3；增加一个定时job任务补偿，检索订单量够，但没有结算的众筹组队记录】
        if (gameCrowdfundingTeamEntity.getTargetCount() - gameCrowdfundingTeamEntity.getCompleteCount() == 1) {
            int updateOrderStatusCount = gameCrowdfundingOrderDao.updateOrderStatus2COMPLETE(gameCrowdfundingTeamEntity.getTeamId());
            if (1 != updateOrderStatusCount) {
                throw new AppException(ResponseCode.UPDATE_ZERO);
            }

            // 查询众筹交易完成外部单号列表
            List<String> outTradeNoList = gameCrowdfundingOrderListDao.queryGameCrowdfundingCompleteOrderOutTradeNoListByTeamId(gameCrowdfundingTeamEntity.getTeamId());

            // 众筹完成写入回调任务记录
            NotifyTask notifyTask = new NotifyTask();
            notifyTask.setActivityId(gameCrowdfundingTeamEntity.getActivityId());
            notifyTask.setTeamId(gameCrowdfundingTeamEntity.getTeamId());
            notifyTask.setNotifyCategory(TaskNotifyCategoryEnumVO.TRADE_SETTLEMENT.getCode());
            notifyTask.setNotifyType(notifyConfigVO.getNotifyType().getCode());
            notifyTask.setNotifyMQ(NotifyTypeEnumVO.MQ.equals(notifyConfigVO.getNotifyType()) ? notifyConfigVO.getNotifyMQ() : null);
            notifyTask.setNotifyUrl(NotifyTypeEnumVO.HTTP.equals(notifyConfigVO.getNotifyType()) ? notifyConfigVO.getNotifyUrl() : null);
            notifyTask.setNotifyCount(0);
            notifyTask.setNotifyStatus(0);
            notifyTask.setUuid(gameCrowdfundingTeamEntity.getTeamId() + Constants.UNDERLINE + TaskNotifyCategoryEnumVO.TRADE_SETTLEMENT.getCode() + Constants.UNDERLINE + tradePaySuccessEntity.getOutTradeNo());

            notifyTask.setParameterJson(JSON.toJSONString(new HashMap<String, Object>() {{
                put("teamId", gameCrowdfundingTeamEntity.getTeamId());
                put("outTradeNoList", outTradeNoList);
            }}));

            notifyTaskDao.insert(notifyTask);

            return NotifyTaskEntity.builder()
                    .teamId(notifyTask.getTeamId())
                    .notifyType(notifyTask.getNotifyType())
                    .notifyMQ(notifyTask.getNotifyMQ())
                    .notifyUrl(notifyTask.getNotifyUrl())
                    .notifyCount(notifyTask.getNotifyCount())
                    .parameterJson(notifyTask.getParameterJson())
                    .uuid(notifyTask.getUuid())
                    .build();
        }

        return null;
    }

    @Override
    public boolean isSCBlackIntercept(String source, String channel) {
        return dccService.isSCBlackIntercept(source, channel);
    }

    @Override
    public List<NotifyTaskEntity> queryUnExecutedNotifyTaskList() {
        List<NotifyTask> notifyTaskList = notifyTaskDao.queryUnExecutedNotifyTaskList();
        if (notifyTaskList.isEmpty()) return new ArrayList<>();

        List<NotifyTaskEntity> notifyTaskEntities = new ArrayList<>();
        for (NotifyTask notifyTask : notifyTaskList) {

            NotifyTaskEntity notifyTaskEntity = NotifyTaskEntity.builder()
                    .teamId(notifyTask.getTeamId())
                    .notifyType(notifyTask.getNotifyType())
                    .notifyMQ(notifyTask.getNotifyMQ())
                    .notifyUrl(notifyTask.getNotifyUrl())
                    .notifyCount(notifyTask.getNotifyCount())
                    .parameterJson(notifyTask.getParameterJson())
                    .uuid(notifyTask.getUuid())
                    .build();

            notifyTaskEntities.add(notifyTaskEntity);
        }

        return notifyTaskEntities;
    }

    @Override
    public List<NotifyTaskEntity> queryUnExecutedNotifyTaskList(String teamId) {
        NotifyTask notifyTask = notifyTaskDao.queryUnExecutedNotifyTaskByTeamId(teamId);
        if (null == notifyTask) return new ArrayList<>();
        return Collections.singletonList(NotifyTaskEntity.builder()
                .teamId(notifyTask.getTeamId())
                .notifyType(notifyTask.getNotifyType())
                .notifyMQ(notifyTask.getNotifyMQ())
                .notifyUrl(notifyTask.getNotifyUrl())
                .notifyCount(notifyTask.getNotifyCount())
                .parameterJson(notifyTask.getParameterJson())
                .uuid(notifyTask.getUuid())
                .build());
    }

    @Override
    public int updateNotifyTaskStatusSuccess(NotifyTaskEntity notifyTaskEntity) {
        NotifyTask notifyTask = NotifyTask.builder()
                .teamId(notifyTaskEntity.getTeamId())
                .uuid(notifyTaskEntity.getUuid())
                .build();
        return notifyTaskDao.updateNotifyTaskStatusSuccess(notifyTask);
    }

    @Override
    public int updateNotifyTaskStatusError(NotifyTaskEntity notifyTaskEntity) {
        NotifyTask notifyTask = NotifyTask.builder()
                .teamId(notifyTaskEntity.getTeamId())
                .uuid(notifyTaskEntity.getUuid())
                .build();
        return notifyTaskDao.updateNotifyTaskStatusError(notifyTask);
    }

    @Override
    public int updateNotifyTaskStatusRetry(NotifyTaskEntity notifyTaskEntity) {
        NotifyTask notifyTask = NotifyTask.builder()
                .teamId(notifyTaskEntity.getTeamId())
                .uuid(notifyTaskEntity.getUuid())
                .build();
        return notifyTaskDao.updateNotifyTaskStatusRetry(notifyTask);
    }

    /**
     * 占用库存。用 Redis 原子计数对比目标量和恢复量，避免超卖。
     */
    @Override
    public boolean occupyTeamStock(String teamStockKey, String recoveryTeamStockKey, Integer target, Integer validTime) {
        // 失败恢复量
        Long recoveryCount = redisService.getAtomicLong(recoveryTeamStockKey);
        recoveryCount = null == recoveryCount ? 0 : recoveryCount;

        // 1. incr 得到值，与总量和恢复量做对比。恢复量为系统失败时候记录的量。
        // 2. 从有组队量开始，相当于已经有了一个占用量，所以要 +1
        long occupy = redisService.incr(teamStockKey) + 1;

        if (occupy > target + recoveryCount) {
            // 超出库存限制时，需要将已经增加的库存减回去，避免库存泄漏
            redisService.decr(teamStockKey);
            return false;
        }

        // 1. 给每个产生的值加锁为兜底设计，虽然incr操作是原子的，基本不会产生一样的值。但在实际生产中，遇到过集群的运维配置问题，以及业务运营配置数据问题，导致incr得到的值相同。
        // 2. validTime + 60分钟，是一个延后时间的设计，让数据保留时间稍微长一些，便于排查问题。
        String lockKey = teamStockKey + Constants.UNDERLINE + occupy;
        Boolean lock = redisService.setNx(lockKey, validTime + 60, TimeUnit.MINUTES);

        if (!lock) {
            log.info("组队库存加锁失败 {}", lockKey);
        }

        return lock;
    }

    @Override
    public void recoveryTeamStock(String recoveryTeamStockKey, Integer validTime) {
        // 首次组队众筹，是没有 teamId 的，所以不需要这个做处理。
        if (StringUtils.isBlank(recoveryTeamStockKey)) return;

        redisService.incr(recoveryTeamStockKey);
    }

    @Override
    @Transactional(timeout = 5000)
    public NotifyTaskEntity unpaid2Refund(GameCrowdfundingRefundAggregate gameCrowdfundingRefundAggregate) {
        TradeRefundOrderEntity tradeRefundOrderEntity = gameCrowdfundingRefundAggregate.getTradeRefundOrderEntity();
        GameCrowdfundingProgressVO gameCrowdfundingProgress = gameCrowdfundingRefundAggregate.getGameCrowdfundingProgress();

        GameCrowdfundingOrderList gameCrowdfundingOrderListReq = new GameCrowdfundingOrderList();
        // 保留userId，企业中往往会根据 userId 作为分库分表路由键，如果将来做分库分表也可以方便处理
        gameCrowdfundingOrderListReq.setUserId(tradeRefundOrderEntity.getUserId());
        gameCrowdfundingOrderListReq.setOrderId(tradeRefundOrderEntity.getOrderId());

        int updateUnpaid2RefundCount = gameCrowdfundingOrderListDao.unpaid2Refund(gameCrowdfundingOrderListReq);
        if (1 != updateUnpaid2RefundCount) {
            log.error("逆向流程-unpaid2Refund，更新订单状态(退单)失败 {} {}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getOrderId());
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        GameCrowdfundingOrder gameCrowdfundingOrderReq = new GameCrowdfundingOrder();
        gameCrowdfundingOrderReq.setTeamId(tradeRefundOrderEntity.getTeamId());
        gameCrowdfundingOrderReq.setLockCount(gameCrowdfundingProgress.getLockCount());

        int updateTeamUnpaid2Refund = gameCrowdfundingOrderDao.unpaid2Refund(gameCrowdfundingOrderReq);
        if (1 != updateTeamUnpaid2Refund) {
            log.error("逆向流程-unpaid2Refund，更新组队记录(退单)失败 {} {}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getOrderId());
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        // 本地消息任务表
        NotifyTask notifyTask = new NotifyTask();
        notifyTask.setActivityId(tradeRefundOrderEntity.getActivityId());
        notifyTask.setTeamId(tradeRefundOrderEntity.getTeamId());
        notifyTask.setNotifyCategory(TaskNotifyCategoryEnumVO.TRADE_UNPAID2REFUND.getCode());
        notifyTask.setNotifyType(NotifyTypeEnumVO.MQ.getCode());
        notifyTask.setNotifyMQ(topic_team_refund);
        notifyTask.setNotifyCount(0);
        notifyTask.setNotifyStatus(0);
        notifyTask.setUuid(tradeRefundOrderEntity.getTeamId() + Constants.UNDERLINE + TaskNotifyCategoryEnumVO.TRADE_UNPAID2REFUND.getCode() + Constants.UNDERLINE + tradeRefundOrderEntity.getOrderId());

        notifyTask.setParameterJson(JSON.toJSONString(new HashMap<String, Object>() {{
            put("type", RefundTypeEnumVO.UNPAID_UNLOCK.getCode());
            put("userId", tradeRefundOrderEntity.getUserId());
            put("teamId", tradeRefundOrderEntity.getTeamId());
            put("orderId", tradeRefundOrderEntity.getOrderId());
            put("outTradeNo", tradeRefundOrderEntity.getOutTradeNo());
            put("activityId", tradeRefundOrderEntity.getActivityId());
        }}));

        notifyTaskDao.insert(notifyTask);

        return NotifyTaskEntity.builder()
                .teamId(notifyTask.getTeamId())
                .notifyType(notifyTask.getNotifyType())
                .notifyMQ(notifyTask.getNotifyMQ())
                .notifyCount(notifyTask.getNotifyCount())
                .parameterJson(notifyTask.getParameterJson())
                .uuid(notifyTask.getUuid())
                .build();
    }

    @Override
    @Transactional(timeout = 5000)
    public NotifyTaskEntity paid2Refund(GameCrowdfundingRefundAggregate gameCrowdfundingRefundAggregate) {
        TradeRefundOrderEntity tradeRefundOrderEntity = gameCrowdfundingRefundAggregate.getTradeRefundOrderEntity();
        GameCrowdfundingProgressVO gameCrowdfundingProgress = gameCrowdfundingRefundAggregate.getGameCrowdfundingProgress();

        GameCrowdfundingOrderList gameCrowdfundingOrderListReq = new GameCrowdfundingOrderList();
        // 保留userId，企业中往往会根据 userId 作为分库分表路由键，如果将来做分库分表也可以方便处理
        gameCrowdfundingOrderListReq.setUserId(tradeRefundOrderEntity.getUserId());
        gameCrowdfundingOrderListReq.setOrderId(tradeRefundOrderEntity.getOrderId());

        int updatePaid2RefundCount = gameCrowdfundingOrderListDao.paid2Refund(gameCrowdfundingOrderListReq);
        if (1 != updatePaid2RefundCount) {
            log.error("逆向流程-paid2Refund，更新订单状态(退单)失败 {} {}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getOrderId());
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        GameCrowdfundingOrder gameCrowdfundingOrderReq = new GameCrowdfundingOrder();
        gameCrowdfundingOrderReq.setTeamId(tradeRefundOrderEntity.getTeamId());
        gameCrowdfundingOrderReq.setLockCount(gameCrowdfundingProgress.getLockCount());
        gameCrowdfundingOrderReq.setCompleteCount(gameCrowdfundingProgress.getCompleteCount());

        int updateTeamPaid2Refund = gameCrowdfundingOrderDao.paid2Refund(gameCrowdfundingOrderReq);
        if (1 != updateTeamPaid2Refund) {
            log.error("逆向流程-paid2Refund，更新组队记录(退单)失败 {} {}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getOrderId());
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        // 本地消息任务表
        NotifyTask notifyTask = new NotifyTask();
        notifyTask.setActivityId(tradeRefundOrderEntity.getActivityId());
        notifyTask.setTeamId(tradeRefundOrderEntity.getTeamId());
        notifyTask.setNotifyCategory(TaskNotifyCategoryEnumVO.TRADE_PAID2REFUND.getCode());
        notifyTask.setNotifyType(NotifyTypeEnumVO.MQ.getCode());
        notifyTask.setNotifyMQ(topic_team_refund);
        notifyTask.setNotifyCount(0);
        notifyTask.setNotifyStatus(0);
        notifyTask.setUuid(tradeRefundOrderEntity.getTeamId() + Constants.UNDERLINE + TaskNotifyCategoryEnumVO.TRADE_PAID2REFUND.getCode() + Constants.UNDERLINE + tradeRefundOrderEntity.getOrderId());

        notifyTask.setParameterJson(JSON.toJSONString(new HashMap<String, Object>() {{
            put("type", RefundTypeEnumVO.PAID_UNFORMED.getCode());
            put("userId", tradeRefundOrderEntity.getUserId());
            put("teamId", tradeRefundOrderEntity.getTeamId());
            put("orderId", tradeRefundOrderEntity.getOrderId());
            put("outTradeNo", tradeRefundOrderEntity.getOutTradeNo());
            put("activityId", tradeRefundOrderEntity.getActivityId());
        }}));

        notifyTaskDao.insert(notifyTask);

        return NotifyTaskEntity.builder()
                .teamId(notifyTask.getTeamId())
                .notifyType(notifyTask.getNotifyType())
                .notifyMQ(notifyTask.getNotifyMQ())
                .notifyCount(notifyTask.getNotifyCount())
                .parameterJson(notifyTask.getParameterJson())
                .uuid(notifyTask.getUuid())
                .build();
    }

    @Override
    @Transactional(timeout = 5000)
    public NotifyTaskEntity paidTeam2Refund(GameCrowdfundingRefundAggregate gameCrowdfundingRefundAggregate) {
        TradeRefundOrderEntity tradeRefundOrderEntity = gameCrowdfundingRefundAggregate.getTradeRefundOrderEntity();
        GameCrowdfundingProgressVO gameCrowdfundingProgress = gameCrowdfundingRefundAggregate.getGameCrowdfundingProgress();
        GameCrowdfundingOrderEnumVO gameCrowdfundingOrderEnumVO = gameCrowdfundingRefundAggregate.getGameCrowdfundingOrderEnumVO();

        GameCrowdfundingOrderList gameCrowdfundingOrderListReq = new GameCrowdfundingOrderList();
        // 保留userId，企业中往往会根据 userId 作为分库分表路由键，如果将来做分库分表也可以方便处理
        gameCrowdfundingOrderListReq.setUserId(tradeRefundOrderEntity.getUserId());
        gameCrowdfundingOrderListReq.setOrderId(tradeRefundOrderEntity.getOrderId());

        int updatePaid2RefundCount = gameCrowdfundingOrderListDao.paidTeam2Refund(gameCrowdfundingOrderListReq);
        if (1 != updatePaid2RefundCount) {
            log.error("逆向流程-paidTeam2Refund，更新订单状态(退单)失败 {} {}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getOrderId());
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        GameCrowdfundingOrder gameCrowdfundingOrderReq = new GameCrowdfundingOrder();
        gameCrowdfundingOrderReq.setTeamId(tradeRefundOrderEntity.getTeamId());
        gameCrowdfundingOrderReq.setLockCount(gameCrowdfundingProgress.getLockCount());
        gameCrowdfundingOrderReq.setCompleteCount(gameCrowdfundingProgress.getCompleteCount());

        // 根据众筹组队量更新状态。组队最后一个人->更新组队失败，组队还有其他人->更新组队完成含退单
        if (GameCrowdfundingOrderEnumVO.COMPLETE_FAIL.equals(gameCrowdfundingOrderEnumVO)) {
            int updateTeamPaid2Refund = gameCrowdfundingOrderDao.paidTeam2Refund(gameCrowdfundingOrderReq);
            if (1 != updateTeamPaid2Refund) {
                log.error("逆向流程-paidTeam2Refund，更新组队记录(退单)失败 {} {}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getOrderId());
                throw new AppException(ResponseCode.UPDATE_ZERO);
            }
        } else if (GameCrowdfundingOrderEnumVO.FAIL.equals(gameCrowdfundingOrderEnumVO)){
            int updateTeamPaid2RefundFail = gameCrowdfundingOrderDao.paidTeam2RefundFail(gameCrowdfundingOrderReq);
            if (1 != updateTeamPaid2RefundFail) {
                log.error("逆向流程-updateTeamPaid2RefundFail，更新组队记录(退单)失败 {} {}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getOrderId());
                throw new AppException(ResponseCode.UPDATE_ZERO);
            }
        }

        // 本地消息任务表
        NotifyTask notifyTask = new NotifyTask();
        notifyTask.setActivityId(tradeRefundOrderEntity.getActivityId());
        notifyTask.setTeamId(tradeRefundOrderEntity.getTeamId());
        notifyTask.setNotifyCategory(TaskNotifyCategoryEnumVO.TRADE_PAID_TEAM2REFUND.getCode());
        notifyTask.setNotifyType(NotifyTypeEnumVO.MQ.getCode());
        notifyTask.setNotifyMQ(topic_team_refund);
        notifyTask.setNotifyCount(0);
        notifyTask.setNotifyStatus(0);
        notifyTask.setUuid(tradeRefundOrderEntity.getTeamId() + Constants.UNDERLINE + TaskNotifyCategoryEnumVO.TRADE_PAID_TEAM2REFUND.getCode() + Constants.UNDERLINE + tradeRefundOrderEntity.getOrderId());

        notifyTask.setParameterJson(JSON.toJSONString(new HashMap<String, Object>() {{
            put("type", RefundTypeEnumVO.PAID_FORMED.getCode());
            put("userId", tradeRefundOrderEntity.getUserId());
            put("teamId", tradeRefundOrderEntity.getTeamId());
            put("orderId", tradeRefundOrderEntity.getOrderId());
            put("outTradeNo", tradeRefundOrderEntity.getOutTradeNo());
            put("activityId", tradeRefundOrderEntity.getActivityId());
        }}));

        notifyTaskDao.insert(notifyTask);

        return NotifyTaskEntity.builder()
                .teamId(notifyTask.getTeamId())
                .notifyType(notifyTask.getNotifyType())
                .notifyMQ(notifyTask.getNotifyMQ())
                .notifyCount(notifyTask.getNotifyCount())
                .parameterJson(notifyTask.getParameterJson())
                .build();
    }

    @Override
    public void refund2AddRecovery(String recoveryTeamStockKey, String orderId) {
        // 如果恢复库存key为空，直接返回
        if (StringUtils.isBlank(recoveryTeamStockKey) || StringUtils.isBlank(orderId)) {
            return;
        }

        // 使用orderId作为锁的key，避免同一订单重复恢复库存
        String lockKey = "refund_lock_" + orderId;
        
        // 尝试获取分布式锁，防止重复操作 30天过期
        Boolean lockAcquired = redisService.setNx(lockKey, 30 * 24 * 60 * 60 * 1000L, TimeUnit.MINUTES);
        
        if (!lockAcquired) {
            log.warn("订单 {} 恢复库存操作已在进行中，跳过重复操作", orderId);
            return;
        }

        try {
            // 在锁保护下执行库存恢复操作
            redisService.incr(recoveryTeamStockKey);
            log.info("订单 {} 恢复库存成功，恢复库存key: {}", orderId, recoveryTeamStockKey);
        } catch (Exception e) {
            log.error("订单 {} 恢复库存失败，恢复库存key: {}", orderId, recoveryTeamStockKey, e);
            // 如果抛异常则释放锁，允许MQ重新消费恢复库存
            redisService.remove(lockKey);
            throw e;
        }

    }

    @Override
    public List<UserGameCrowdfundingOrderDetailEntity> queryTimeoutUnpaidOrderList() {
        List<GameCrowdfundingOrderList> gameCrowdfundingOrderLists = gameCrowdfundingOrderListDao.queryTimeoutUnpaidOrderList();
        if (null == gameCrowdfundingOrderLists || gameCrowdfundingOrderLists.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取所有teamId
        Set<String> teamIds = gameCrowdfundingOrderLists.stream()
                .map(GameCrowdfundingOrderList::getTeamId)
                .collect(Collectors.toSet());
        
        // 查询团队信息
        List<GameCrowdfundingOrder> gameCrowdfundingOrders = gameCrowdfundingOrderDao.queryGameCrowdfundingTeamByTeamIds(teamIds);
        if (null == gameCrowdfundingOrders || gameCrowdfundingOrders.isEmpty()) {
            return new ArrayList<>();
        }
        
        Map<String, GameCrowdfundingOrder> gameCrowdfundingOrderMap = gameCrowdfundingOrders.stream()
                .collect(Collectors.toMap(GameCrowdfundingOrder::getTeamId, order -> order));
        
        // 转换数据
        List<UserGameCrowdfundingOrderDetailEntity> userGameCrowdfundingOrderDetailEntities = new ArrayList<>();
        for (GameCrowdfundingOrderList gameCrowdfundingOrderList : gameCrowdfundingOrderLists) {
            String teamId = gameCrowdfundingOrderList.getTeamId();
            GameCrowdfundingOrder gameCrowdfundingOrder = gameCrowdfundingOrderMap.get(teamId);
            if (null == gameCrowdfundingOrder) continue;
            
            UserGameCrowdfundingOrderDetailEntity userGameCrowdfundingOrderDetailEntity = UserGameCrowdfundingOrderDetailEntity.builder()
                    .userId(gameCrowdfundingOrderList.getUserId())
                    .teamId(gameCrowdfundingOrder.getTeamId())
                    .activityId(gameCrowdfundingOrder.getActivityId())
                    .targetCount(gameCrowdfundingOrder.getTargetCount())
                    .completeCount(gameCrowdfundingOrder.getCompleteCount())
                    .lockCount(gameCrowdfundingOrder.getLockCount())
                    .validStartTime(gameCrowdfundingOrder.getValidStartTime())
                    .validEndTime(gameCrowdfundingOrder.getValidEndTime())
                    .outTradeNo(gameCrowdfundingOrderList.getOutTradeNo())
                    .source(gameCrowdfundingOrderList.getSource())
                    .channel(gameCrowdfundingOrderList.getChannel())
                    .build();
            
            userGameCrowdfundingOrderDetailEntities.add(userGameCrowdfundingOrderDetailEntity);
        }
        
        return userGameCrowdfundingOrderDetailEntities;
    }

}
