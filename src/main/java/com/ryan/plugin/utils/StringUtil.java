package com.ryan.plugin.utils;

import java.util.Arrays;

public class StringUtil {

    public static String captureName(String name) {
        char[] cs = name.toCharArray();
        cs[0] += 32;
        return String.valueOf(cs);
    }

    public static String getFieldName(String getter) {
        String substring = getter.substring(3);
        return captureName(substring);
    }
}
