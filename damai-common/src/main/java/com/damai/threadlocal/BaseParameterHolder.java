package com.damai.threadlocal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BaseParameterHolder {

    private static final ThreadLocal<Map<String, String>> THREAD_LOCAL_MAP = new ThreadLocal<>();

    public static void setParameter(String name, String value) {
        Map<String, String> map = THREAD_LOCAL_MAP.get();
        if(map == null) {
            map = new HashMap<>(64);
        }
        map.put(name, value);
        THREAD_LOCAL_MAP.set(map);
    }

    public static String getParameter(String name) {
        return  Optional.ofNullable(THREAD_LOCAL_MAP.get()).map(map -> map.get(name)).orElse(null);  // Optional.ofNullable()接收的值可能是null也可能不是null
    }

    public static void removeParameter(String name) {
        Map<String, String> map = THREAD_LOCAL_MAP.get();
        if(map != null) {
            map.remove(name);
        }
    }

    public static ThreadLocal<Map<String, String>> getThreadLocal() {
        return THREAD_LOCAL_MAP;
    }

    public static Map<String, String> getParameterMap() {
        Map<String, String> map = THREAD_LOCAL_MAP.get();
        if(map == null) {
            map = new HashMap<>(64);
        }
        return map;
    }

    public static void setParameterMap(Map<String, String> map) {
        THREAD_LOCAL_MAP.set(map);
    }

    public static void removeParameterMap() {
        THREAD_LOCAL_MAP.remove();
    }
}
