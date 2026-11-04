package com.indiefund.market.infrastructure.dao;

import com.indiefund.market.infrastructure.dao.po.CrowdTags;
import com.indiefund.market.infrastructure.dao.po.CrowdTagsJob;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description 人群标签
 * @create 2024-12-28 11:49
 */
@Mapper
public interface ICrowdTagsDao {

    void updateCrowdTagsStatistics(CrowdTags crowdTagsReq);

}
