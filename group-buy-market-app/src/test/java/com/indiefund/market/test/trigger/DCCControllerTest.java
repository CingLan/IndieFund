package com.indiefund.market.test.trigger;

import com.indiefund.market.api.IDCCService;
import com.indiefund.market.domain.activity.model.entity.MarketProductEntity;
import com.indiefund.market.domain.activity.model.entity.TrialBalanceEntity;
import com.indiefund.market.domain.activity.service.IIndexGameCrowdfundingMarketService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * @description 动态配置管理测试
 * @create 2025-01-03 19:43
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class DCCControllerTest {

    @Resource
    private IDCCService dccService;

    @Resource
    private IIndexGameCrowdfundingMarketService indexGameCrowdfundingMarketService;

    @Test
    public void test_updateConfig() {
        // 动态调整配置
        dccService.updateConfig("downgradeSwitch", "1");
    }

    @Test
    public void test_updateConfig2indexMarketTrial() throws Exception {
        // 动态调整配置
        dccService.updateConfig("downgradeSwitch", "1");
        // 超时等待异步
        Thread.sleep(1000);

        // 营销验证
        MarketProductEntity marketProductEntity = new MarketProductEntity();
        marketProductEntity.setUserId("indie_user");
        marketProductEntity.setSource("s01");
        marketProductEntity.setChannel("c01");
        marketProductEntity.setGoodsId("9890001");

        TrialBalanceEntity trialBalanceEntity = indexGameCrowdfundingMarketService.indexMarketTrial(marketProductEntity);
        log.info("请求参数:{}", JSON.toJSONString(marketProductEntity));
        log.info("返回结果:{}", JSON.toJSONString(trialBalanceEntity));
    }


}
