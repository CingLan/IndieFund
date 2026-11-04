package com.indiefund.market.test.infrastructure.dao;

import com.indiefund.market.infrastructure.dao.IGameCrowdfundingDiscountDao;
import com.indiefund.market.infrastructure.dao.po.GameCrowdfundingDiscount;
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
public class GameCrowdfundingDiscountDaoTest {

    @Resource
    private IGameCrowdfundingDiscountDao gameCrowdfundingDiscountDao;

    @Test
    public void test_queryGameCrowdfundingDiscountList(){
        List<GameCrowdfundingDiscount> gameCrowdfundingDiscounts = gameCrowdfundingDiscountDao.queryGameCrowdfundingDiscountList();
        log.info("测试结果:{}", JSON.toJSONString(gameCrowdfundingDiscounts));
    }

}
