package com.indiefund.market.infrastructure.dao;

import com.indiefund.market.infrastructure.dao.po.GameTier;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description 商品查询
 * @create 2024-12-21 10:48
 */
@Mapper
public interface IGameTierDao {

    GameTier queryGameTierByGoodsId(String goodsId);

}
