package com.t1908e.memeportalapi.util;

import java.util.Random;

public class RandomUtil {
    public static String generateVerifyCode() {
        return String.valueOf(new Random().nextInt(899999) + 100000);
    }

}
