package com.indiefund.market.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum ResponseCode {

    SUCCESS("0000", "成功"),
    UN_ERROR("0001", "未知失败"),
    ILLEGAL_PARAMETER("0002", "非法参数"),
    INDEX_EXCEPTION("0003", "唯一索引冲突"),
    UPDATE_ZERO("0004", "更新记录为0"),
    HTTP_EXCEPTION("0005", "HTTP接口调用异常"),
    RATE_LIMITER("0006", "接口限流"),

    E0001("E0001", "不存在对应的折扣计算服务"),
    E0002("E0002", "无众筹档位支持配置"),
    E0003("E0003", "众筹活动降级拦截"),
    E0004("E0004", "众筹活动切量拦截"),
    E0005("E0005", "众筹组队失败，记录更新为0"),
    E0006("E0006", "众筹组队完结，锁单量已达成"),
    E0007("E0007", "众筹人群限定，不可参与"),
    E0008("E0008", "众筹组队失败，缓存库存不足"),

    E0101("E0101", "众筹活动未生效"),
    E0102("E0102", "不在众筹活动有效时间内"),
    E0103("E0103", "当前用户参与此众筹次数已达上限"),
    E0104("E0104", "不存在的外部交易单号或用户已退单"),
    E0105("E0105", "SC渠道黑名单拦截"),
    E0106("E0106", "订单交易时间不在众筹有效时间范围内"),

    ;

    private String code;
    private String info;

}
