package com.damai.config;

import com.damai.util.DateUtils;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

public class DateJsonDeserializer extends JsonDeserializer<Date> {

    private static final Pattern P = Pattern.compile("^[0-9]*");  // 判断是否是数字
    private static final List<String> FORMAT = new ArrayList<>(4);
    static {
        FORMAT.add("yyyy-MM");
        FORMAT.add("yyyy-MM-dd");
        FORMAT.add("yyyy-MM-dd HH:mm");
        FORMAT.add("yyyy-MM-dd HH:mm:ss");
        FORMAT.add("yyyy/MM/dd HH:mm:ss");
    }

    @Override
    public Date deserialize(JsonParser p, DeserializationContext context) throws IOException, JacksonException {
        Date convertDate = null;
        String str = p.getText();

        if(str == null || str.isEmpty()) {
            return null;
        }

        if(isNum(str)) {
            convertDate = new Date(Long.valueOf(str));
        }else {
            if (str.matches("^\\d{4}-\\d{1,2}$")) {
                return DateUtils.parse(str, FORMAT.get(0));
            } else if (str.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$")) {
                return DateUtils.parse(str, FORMAT.get(1));
            } else if (str.matches("^\\d{4}-\\d{1,2}-\\d{1,2} {1}\\d{1,2}:\\d{1,2}$")) {
                return DateUtils.parse(str, FORMAT.get(2));
            } else if (str.matches("^\\d{4}-\\d{1,2}-\\d{1,2} {1}\\d{1,2}:\\d{1,2}:\\d{1,2}$")) {
                return DateUtils.parse(str, FORMAT.get(3));
            }  else if (str.matches("^\\d{4}/\\d{1,2}/\\d{1,2} {1}\\d{1,2}:\\d{1,2}:\\d{1,2}$")) {
                return DateUtils.parse(str, FORMAT.get(4));
            }else {
                throw new IllegalArgumentException("Invalid boolean value '" + str + "'");
            }
        }

        return convertDate;
    }

    /**
     * 校验字符串是否是数字
     */
    public static boolean isNum(String number){
        Matcher m = P.matcher(number);
        return m.matches();
    }
}
