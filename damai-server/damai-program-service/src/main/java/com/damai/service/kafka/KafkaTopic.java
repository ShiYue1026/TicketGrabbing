package com.damai.service.kafka;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class KafkaTopic {

    @Value("${spring.kafka.topic:default}")
    private String topic;

}
