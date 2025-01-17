package com.damai.kafka;

import com.damai.core.SpringUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApiDataMessageSend {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${spring.kafka.topic}")
    private String topic;

    public void sendMessage(String message) {
        log.info("sendMessage message : {}", message);
        kafkaTemplate.send(SpringUtil.getPrefixDistinctionName() + "-" + topic,message);
    }
}

