package com.indiefund.market.test.infrastructure.dao;

import com.indiefund.market.infrastructure.dao.IGameCrowdfundingActivityDao;
import com.indiefund.market.infrastructure.dao.po.GameCrowdfundingActivity;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class GameCrowdfundingActivityDaoTest {

    @Resource
    private IGameCrowdfundingActivityDao gameCrowdfundingActivityDao;

    @Test
    public void test_queryGameCrowdfundingActivityList() {
        List<GameCrowdfundingActivity> gameCrowdfundingActivities = gameCrowdfundingActivityDao.queryGameCrowdfundingActivityList();
        log.info("测试结果:{}", JSON.toJSONString(gameCrowdfundingActivities));
    }

}
