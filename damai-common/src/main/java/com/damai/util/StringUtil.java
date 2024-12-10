package com.damai.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class StringUtil {

    public static boolean isNotEmpty(String str) {
        return (str != null && !str.isEmpty() && !str.trim().isEmpty() && !"null".equalsIgnoreCase(str.trim())
                && !"undefined".equalsIgnoreCase(str.trim()) && !"NULL".equalsIgnoreCase(str.trim()));
    }

    public static boolean isEmpty(String str) {
        return !StringUtil.isNotEmpty(str);
    }

    public static String inputStreamConvertString(InputStream is) {
        ByteArrayOutputStream baos = null;
        String result = null;

        try{
            if(is != null){
                baos = new ByteArrayOutputStream();
                int i;
                while ((i = is.read()) != -1) {
                    baos.write(i);
                }
                result = baos.toString();
            }
        }
        catch(Exception e){
            throw new RuntimeException("流转换为字符串失败！");
        }
        finally {
            if(baos != null){
                try {
                    baos.close();
                } catch (IOException e) {
                    log.error("关闭流失败！");
                }
            }
        }
        return result;
    }
}
