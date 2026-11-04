package com.indiefund.market.infrastructure.gateway;

import com.indiefund.market.types.enums.ResponseCode;
import com.indiefund.market.types.exception.AppException;
import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @description 众筹回调服务
 * @create 2025-01-31 09:12
 */
@Slf4j
@Service
public class GameCrowdfundingNotifyService {

    @Resource
    private OkHttpClient okHttpClient;

    public String gameCrowdfundingNotify(String apiUrl, String notifyRequestDTOJSON) throws Exception {
        try {
            // 1. 构建参数
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, notifyRequestDTOJSON);
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .post(body)
                    .addHeader("content-type", "application/json")
                    .build();

            // 2. 调用接口
            Response response = okHttpClient.newCall(request).execute();

            // 3. 返回结果
            return response.body().string();
        } catch (Exception e) {
            log.error("众筹回调 HTTP 接口服务异常 {}", apiUrl, e);
            throw new AppException(ResponseCode.HTTP_EXCEPTION);
        }
    }

}
