package com.damai.pay.init;

import com.damai.initialize.base.AbstractApplicationInitializingBeanHandler;
import com.damai.pay.PayStrategyContext;
import com.damai.pay.PayStrategyHandler;
import lombok.AllArgsConstructor;
import org.springframework.context.ConfigurableApplicationContext;
import java.util.Map;

@AllArgsConstructor
public class PayStrategyInitHandler extends AbstractApplicationInitializingBeanHandler {

    private final PayStrategyContext payStrategyContext;

    @Override
    public Integer executeOrder() {
        return 1;
    }

    @Override
    public void executeInit(ConfigurableApplicationContext context) {
        Map<String, PayStrategyHandler> payStrategyHandlerMap = context.getBeansOfType(PayStrategyHandler.class);
        for (Map.Entry<String, PayStrategyHandler> entry : payStrategyHandlerMap.entrySet()) {
            payStrategyContext.put(entry.getValue().getChannel(), entry.getValue());
        }
    }
}
