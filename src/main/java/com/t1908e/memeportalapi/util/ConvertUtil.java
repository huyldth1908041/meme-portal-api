package com.t1908e.memeportalapi.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ConvertUtil {
    public static Date convertStringToJavaDate(String dateString, String formatPattern) throws ParseException {
        Date result = new SimpleDateFormat(formatPattern).parse(dateString);
        return result;
    }
}
