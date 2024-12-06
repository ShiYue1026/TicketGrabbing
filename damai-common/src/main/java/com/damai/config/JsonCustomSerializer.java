package com.damai.config;

import cn.hutool.core.date.DateTime;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class JsonCustomSerializer extends BeanSerializerModifier {

    /**
     改变 Java Bean 中每个字段的序列化行为
     */
    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties){
        for (BeanPropertyWriter writer : beanProperties) {
            JsonSerializer<Object> js = judgeType(writer);
            if(js != null) {
                writer.assignNullSerializer(js);
            }
        }
        return beanProperties;
    }

    /**
     根据 Java Bean 中字段的类型，返回不同的自定义 JSON 序列化器。
     */
    public JsonSerializer<Object> judgeType(BeanPropertyWriter writer){
        JavaType javaType = writer.getType();
        Class<?> clazz = javaType.getRawClass();

        if(String.class.isAssignableFrom(clazz)){
            return new JsonSerializer<Object>() {
                @Override
                public void serialize(Object value, JsonGenerator gen, SerializerProvider serializer) throws IOException {
                    gen.writeString("");
                }
            };
        }

        if(Number.class.isAssignableFrom(clazz)){
            return new JsonSerializer<Object>() {
                @Override
                public void serialize(Object value, JsonGenerator gen, SerializerProvider serializer) throws IOException {
                    gen.writeString("");
                }
            };
        }

        if(Boolean.class.isAssignableFrom(clazz)){
            return new JsonSerializer<Object>() {
                @Override
                public void serialize(Object value, JsonGenerator gen, SerializerProvider serializer) throws IOException {
                    gen.writeBoolean(false);
                }
            };
        }

        if(Date.class.isAssignableFrom(clazz)){
            return new JsonSerializer<Object>() {
                @Override
                public void serialize(Object value, JsonGenerator gen, SerializerProvider serializer) throws IOException {
                    gen.writeString("");
                }
            };
        }

        if(DateTime.class.equals(clazz)){
            return new JsonSerializer<Object>() {
                @Override
                public void serialize(Object value, JsonGenerator gen, SerializerProvider serializer) throws IOException {
                    gen.writeString("");
                }
            };
        }

        if(clazz.isArray() || clazz.equals(List.class) || clazz.equals(Set.class)){
            return new JsonSerializer<Object>() {
                @Override
                public void serialize(Object value, JsonGenerator gen, SerializerProvider serializer) throws IOException {
                    gen.writeStartArray();  // '['
                    gen.writeEndArray();  // ']'
                }
            };
        }

        return null;
    }

}
