package com.indiefund.market.infrastructure.dao;

import com.indiefund.market.infrastructure.dao.po.GameCrowdfundingDiscount;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @description 折扣配置Dao
 * @create 2024-12-07 10:10
 */
@Mapper
public interface IGameCrowdfundingDiscountDao {

    List<GameCrowdfundingDiscount> queryGameCrowdfundingDiscountList();

    GameCrowdfundingDiscount queryGameCrowdfundingActivityDiscountByDiscountId(String discountId);

}
