package com.damai.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.core.Ordered;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

public class JacksonCustom implements Jackson2ObjectMapperBuilderCustomizer, Ordered {

    private final String dateTimeFormat = "yyyy-MM-dd HH:mm:ss"; // 默认时间格式

    @Override
    public void customize(Jackson2ObjectMapperBuilder builder) {
        builder.serializationInclusion(JsonInclude.Include.ALWAYS);  // 序列化时总是包含所有字段，不管其值是否为 null

        builder.featuresToEnable(Feature.ALLOW_SINGLE_QUOTES);
        builder.featuresToEnable(Feature.ALLOW_UNQUOTED_FIELD_NAMES);

        SimpleModule[] simpleModules = new SimpleModule[9];

        simpleModules[0] = new SimpleModule().setSerializerModifier(new JsonCustomSerializer());  // 空值处理
        simpleModules[1] = new SimpleModule().addSerializer(Date.class, new JsonSerializer<Date>(){  // 处理日期格式
            @Override
            public void serialize(Date date, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                SimpleDateFormat sdf = new SimpleDateFormat(dateTimeFormat);
                String newDate = sdf.format(date);
                gen.writeString(newDate);
            }
        });

        simpleModules[2] = new SimpleModule().addDeserializer(Date.class, new DateJsonDeserializer());
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat);
        simpleModules[3] = new SimpleModule().addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        simpleModules[4] = new SimpleModule().addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
        String dateFormat = "yyyy-MM-dd";
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(dateFormat);
        simpleModules[5] = new SimpleModule().addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        simpleModules[6] = new SimpleModule().addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));
        String timeFormat = "HH:mm:ss";
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(timeFormat);
        simpleModules[7] = new SimpleModule().addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
        simpleModules[8] = new SimpleModule().addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));

        builder.modules(simpleModules);

        builder.timeZone(TimeZone.getDefault());
        builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        builder.featuresToEnable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature());
        builder.featuresToEnable(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS.mappedFeature());

    }

    @Override
    public int getOrder() {
        return 1;
    }
}
