package com.indiefund.market.infrastructure.dao;

import com.indiefund.market.infrastructure.dao.po.SCGameTierActivity;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description 渠道商品活动配置关联表Dao
 * @create 2025-01-01 09:30
 */
@Mapper
public interface ISCGameTierActivityDao {

    SCGameTierActivity querySCGameTierActivityBySCGoodsId(SCGameTierActivity scGameTierActivity);

}
