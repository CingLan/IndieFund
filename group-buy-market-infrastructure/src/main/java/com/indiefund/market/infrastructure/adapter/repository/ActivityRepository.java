package com.indiefund.market.infrastructure.adapter.repository;

import com.indiefund.market.domain.activity.adapter.repository.IActivityRepository;
import com.indiefund.market.domain.activity.model.entity.UserGameCrowdfundingOrderDetailEntity;
import com.indiefund.market.domain.activity.model.valobj.*;
import com.indiefund.market.infrastructure.dao.*;
import com.indiefund.market.infrastructure.dao.po.*;
import com.indiefund.market.infrastructure.dcc.DCCService;
import com.indiefund.market.infrastructure.redis.IRedisService;
import org.redisson.api.RBitSet;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description 活动仓储
 * @create 2024-12-21 10:10
 */
@Repository
public class ActivityRepository extends AbstractRepository implements IActivityRepository {

    @Resource
    private IGameCrowdfundingActivityDao gameCrowdfundingActivityDao;
    @Resource
    private IGameCrowdfundingDiscountDao gameCrowdfundingDiscountDao;
    @Resource
    private IGameTierDao gameTierDao;
    @Resource
    private ISCGameTierActivityDao gameTierActivityDao;
    @Resource
    private IRedisService redisService;
    @Resource
    private DCCService dccService;
    @Resource
    private IGameCrowdfundingOrderDao gameCrowdfundingOrderDao;
    @Resource
    private IGameCrowdfundingOrderListDao gameCrowdfundingOrderListDao;

    @Override
    public GameCrowdfundingActivityDiscountVO queryGameCrowdfundingActivityDiscountVO(Long activityId) {
        // 优先从缓存获取&写缓存，注意如果实现了后台配置，在更新时要更库，删缓存。
        GameCrowdfundingActivity gameCrowdfundingActivityRes = getFromCacheOrDb(GameCrowdfundingActivity.cacheRedisKey(activityId),
                () -> gameCrowdfundingActivityDao.queryValidGameCrowdfundingActivityId(activityId));
        if (null == gameCrowdfundingActivityRes) return null;

        String discountId = gameCrowdfundingActivityRes.getDiscountId();

        // 优先从缓存获取&写缓存
        GameCrowdfundingDiscount gameCrowdfundingDiscountRes = getFromCacheOrDb(GameCrowdfundingDiscount.cacheRedisKey(discountId),
                () -> gameCrowdfundingDiscountDao.queryGameCrowdfundingActivityDiscountByDiscountId(discountId));
        if (null == gameCrowdfundingDiscountRes) return null;

        GameCrowdfundingActivityDiscountVO.GameCrowdfundingDiscount gameCrowdfundingDiscount = GameCrowdfundingActivityDiscountVO.GameCrowdfundingDiscount.builder()
                .discountName(gameCrowdfundingDiscountRes.getDiscountName())
                .discountDesc(gameCrowdfundingDiscountRes.getDiscountDesc())
                .discountType(DiscountTypeEnum.get(gameCrowdfundingDiscountRes.getDiscountType()))
                .marketPlan(gameCrowdfundingDiscountRes.getMarketPlan())
                .marketExpr(gameCrowdfundingDiscountRes.getMarketExpr())
                .tagId(gameCrowdfundingDiscountRes.getTagId())
                .build();

        return GameCrowdfundingActivityDiscountVO.builder()
                .activityId(gameCrowdfundingActivityRes.getActivityId())
                .activityName(gameCrowdfundingActivityRes.getActivityName())
                .gameCrowdfundingDiscount(gameCrowdfundingDiscount)
                .groupType(gameCrowdfundingActivityRes.getGroupType())
                .takeLimitCount(gameCrowdfundingActivityRes.getTakeLimitCount())
                .target(gameCrowdfundingActivityRes.getTarget())
                .validTime(gameCrowdfundingActivityRes.getValidTime())
                .status(gameCrowdfundingActivityRes.getStatus())
                .startTime(gameCrowdfundingActivityRes.getStartTime())
                .endTime(gameCrowdfundingActivityRes.getEndTime())
                .tagId(gameCrowdfundingActivityRes.getTagId())
                .tagScope(gameCrowdfundingActivityRes.getTagScope())
                .build();
    }

    @Override
    public GameTierVO queryGameTierByGoodsId(String goodsId) {
        GameTier gameTier = gameTierDao.queryGameTierByGoodsId(goodsId);
        if (null == gameTier) return null;
        return GameTierVO.builder()
                .goodsId(gameTier.getGoodsId())
                .goodsName(gameTier.getGoodsName())
                .originalPrice(gameTier.getOriginalPrice())
                .build();
    }

    @Override
    public SCGameTierActivityVO querySCGameTierActivityBySCGoodsId(String source, String channel, String goodsId) {
        SCGameTierActivity scGameTierActivityReq = new SCGameTierActivity();
        scGameTierActivityReq.setSource(source);
        scGameTierActivityReq.setChannel(channel);
        scGameTierActivityReq.setGoodsId(goodsId);

        SCGameTierActivity scGameTierActivity = gameTierActivityDao.querySCGameTierActivityBySCGoodsId(scGameTierActivityReq);
        if (null == scGameTierActivity) return null;

        return SCGameTierActivityVO.builder()
                .source(scGameTierActivity.getSource())
                .chanel(scGameTierActivity.getChannel())
                .activityId(scGameTierActivity.getActivityId())
                .goodsId(scGameTierActivity.getGoodsId())
                .build();
    }

    @Override
    public boolean isTagCrowdRange(String tagId, String userId) {
        RBitSet bitSet = redisService.getBitSet(tagId);
        if (!bitSet.isExists()) return true;
        // 判断用户是否存在人群中
        return bitSet.get(redisService.getIndexFromUserId(userId));
    }

    @Override
    public boolean downgradeSwitch() {
        return dccService.isDowngradeSwitch();
    }

    @Override
    public boolean cutRange(String userId) {
        return dccService.isCutRange(userId);
    }

    @Override
    public List<UserGameCrowdfundingOrderDetailEntity> queryInProgressUserGameCrowdfundingOrderDetailListByOwner(Long activityId, String userId, Integer ownerCount) {
        // 1. 根据用户ID、活动ID，查询用户参与的众筹队伍
        GameCrowdfundingOrderList gameCrowdfundingOrderListReq = new GameCrowdfundingOrderList();
        gameCrowdfundingOrderListReq.setActivityId(activityId);
        gameCrowdfundingOrderListReq.setUserId(userId);
        gameCrowdfundingOrderListReq.setCount(ownerCount);
        List<GameCrowdfundingOrderList> gameCrowdfundingOrderLists = gameCrowdfundingOrderListDao.queryInProgressUserGameCrowdfundingOrderDetailListByUserId(gameCrowdfundingOrderListReq);
        if (null == gameCrowdfundingOrderLists || gameCrowdfundingOrderLists.isEmpty()) return null;

        // 2. 过滤队伍获取 TeamId
        Set<String> teamIds = gameCrowdfundingOrderLists.stream()
                .map(GameCrowdfundingOrderList::getTeamId)
                .filter(teamId -> teamId != null && !teamId.isEmpty()) // 过滤非空和非空字符串
                .collect(Collectors.toSet());

        // 3. 查询队伍明细，组装Map结构
        List<GameCrowdfundingOrder> gameCrowdfundingOrders = gameCrowdfundingOrderDao.queryGameCrowdfundingProgressByTeamIds(teamIds);
        if (null == gameCrowdfundingOrders || gameCrowdfundingOrders.isEmpty()) return null;

        Map<String, GameCrowdfundingOrder> gameCrowdfundingOrderMap = gameCrowdfundingOrders.stream()
                .collect(Collectors.toMap(GameCrowdfundingOrder::getTeamId, order -> order));

        // 4. 转换数据
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
                    .build();

            userGameCrowdfundingOrderDetailEntities.add(userGameCrowdfundingOrderDetailEntity);
        }

        return userGameCrowdfundingOrderDetailEntities;
    }

    @Override
    public List<UserGameCrowdfundingOrderDetailEntity> queryInProgressUserGameCrowdfundingOrderDetailListByRandom(Long activityId, String userId, Integer randomCount) {
        // 1. 根据用户ID、活动ID，查询用户参与的众筹队伍
        GameCrowdfundingOrderList gameCrowdfundingOrderListReq = new GameCrowdfundingOrderList();
        gameCrowdfundingOrderListReq.setActivityId(activityId);
        gameCrowdfundingOrderListReq.setUserId(userId);
        gameCrowdfundingOrderListReq.setCount(randomCount * 2); // 查询2倍的量，之后其中 randomCount 数量
        List<GameCrowdfundingOrderList> gameCrowdfundingOrderLists = gameCrowdfundingOrderListDao.queryInProgressUserGameCrowdfundingOrderDetailListByRandom(gameCrowdfundingOrderListReq);
        if (null == gameCrowdfundingOrderLists || gameCrowdfundingOrderLists.isEmpty()) return null;

        // 判断总量是否大于 randomCount
        if (gameCrowdfundingOrderLists.size() > randomCount) {
            // 随机打乱列表
            Collections.shuffle(gameCrowdfundingOrderLists);
            // 获取前 randomCount 个元素
            gameCrowdfundingOrderLists = gameCrowdfundingOrderLists.subList(0, randomCount);
        }

        // 2. 过滤队伍获取 TeamId
        Set<String> teamIds = gameCrowdfundingOrderLists.stream()
                .map(GameCrowdfundingOrderList::getTeamId)
                .filter(teamId -> teamId != null && !teamId.isEmpty()) // 过滤非空和非空字符串
                .collect(Collectors.toSet());

        // 3. 查询队伍明细，组装Map结构
        List<GameCrowdfundingOrder> gameCrowdfundingOrders = gameCrowdfundingOrderDao.queryGameCrowdfundingProgressByTeamIds(teamIds);
        if (null == gameCrowdfundingOrders || gameCrowdfundingOrders.isEmpty()) return null;

        Map<String, GameCrowdfundingOrder> gameCrowdfundingOrderMap = gameCrowdfundingOrders.stream()
                .collect(Collectors.toMap(GameCrowdfundingOrder::getTeamId, order -> order));

        // 4. 转换数据
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
                    .build();

            userGameCrowdfundingOrderDetailEntities.add(userGameCrowdfundingOrderDetailEntity);
        }

        return userGameCrowdfundingOrderDetailEntities;
    }

    @Override
    public TeamStatisticVO queryTeamStatisticByActivityId(Long activityId) {
        // 1. 根据活动ID查询众筹队伍
        List<GameCrowdfundingOrderList> gameCrowdfundingOrderLists = gameCrowdfundingOrderListDao.queryInProgressUserGameCrowdfundingOrderDetailListByActivityId(activityId);

        if (null == gameCrowdfundingOrderLists || gameCrowdfundingOrderLists.isEmpty()) {
            return new TeamStatisticVO(0, 0, 0);
        }

        // 2. 过滤队伍获取 TeamId
        Set<String> teamIds = gameCrowdfundingOrderLists.stream()
                .map(GameCrowdfundingOrderList::getTeamId)
                .filter(teamId -> teamId != null && !teamId.isEmpty()) // 过滤非空和非空字符串
                .collect(Collectors.toSet());

        // 3. 统计数据
        Integer allTeamCount = gameCrowdfundingOrderDao.queryAllTeamCount(teamIds);
        Integer allTeamCompleteCount = gameCrowdfundingOrderDao.queryAllTeamCompleteCount(teamIds);
        Integer allTeamUserCount = gameCrowdfundingOrderDao.queryAllUserCount(teamIds);

        // 4. 构建对象
        return TeamStatisticVO.builder()
                .allTeamCount(allTeamCount)
                .allTeamCompleteCount(allTeamCompleteCount)
                .allTeamUserCount(allTeamUserCount)
                .build();
    }

}
