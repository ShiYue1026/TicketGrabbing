package com.damai.pay.autoconfig;

import com.alipay.api.*;
import com.damai.pay.PayStrategyContext;
import com.damai.pay.PayStrategyHandler;
import com.damai.pay.alipay.AlipayStrategyHandler;
import com.damai.pay.alipay.config.AlipayProperties;
import com.damai.pay.init.PayStrategyInitHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@EnableConfigurationProperties(AlipayProperties.class)
public class PayAutoConfig {

    @Bean
    public AlipayClient alipayClient(AlipayProperties alipayProperties) throws AlipayApiException {
        AlipayConfig alipayConfig = new AlipayConfig();
        alipayConfig.setServerUrl(alipayProperties.getGatewayUrl());
        alipayConfig.setAppId(alipayProperties.getAppId());
        alipayConfig.setPrivateKey(alipayProperties.getMerchantPrivateKey());
        alipayConfig.setFormat(AlipayConstants.FORMAT_JSON);
        alipayConfig.setCharset(AlipayConstants.CHARSET_UTF8);
        alipayConfig.setAlipayPublicKey(alipayProperties.getAlipayPublicKey());
        alipayConfig.setSignType(AlipayConstants.SIGN_TYPE_RSA2);

        return new DefaultAlipayClient(alipayConfig);
    }

    @Bean
    public PayStrategyContext payStrategyContext() {
        return new PayStrategyContext();
    }

    @Bean
    public PayStrategyInitHandler payStrategyInitHandler(PayStrategyContext payStrategyContext) {
        return new PayStrategyInitHandler(payStrategyContext);
    }

    @Bean
    public AlipayStrategyHandler alipayCall(AlipayClient alipayClient, AlipayProperties alipayProperties) {
        return new AlipayStrategyHandler(alipayClient,alipayProperties);
    }
}
