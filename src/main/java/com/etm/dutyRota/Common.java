package com.etm.dutyRota;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

public class Common {

    public int getDBResultInt(Session session, String sql, String selectName) {
        int resultCount = 0;
        try {
            NativeQuery<?> query = session.createNativeQuery(sql);
            query.unwrap(NativeQuery.class)
                 .addScalar(selectName, org.hibernate.type.StringType.INSTANCE)
                 .setResultTransformer(org.hibernate.transform.AliasToEntityMapResultTransformer.INSTANCE);

            List<?> rawList = query.list();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> resultList = (List<Map<String, Object>>) (List<?>) rawList;

            if (resultList != null && !resultList.isEmpty()) {
                Object val = resultList.get(0).get(selectName);
                resultCount = Integer.parseInt(val != null ? val.toString() : "0");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultCount;
    }

    public String getDBResultStr(Session session, String sql, String selectName) {
        String result = "";
        try {
            NativeQuery<?> query = session.createNativeQuery(sql);
            query.unwrap(NativeQuery.class)
                 .addScalar(selectName, org.hibernate.type.StringType.INSTANCE)
                 .setResultTransformer(org.hibernate.transform.AliasToEntityMapResultTransformer.INSTANCE);

            List<?> rawList = query.list();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> resultList = (List<Map<String, Object>>) (List<?>) rawList;

            if (resultList != null && !resultList.isEmpty()) {
                Object val = resultList.get(0).get(selectName);
                result = val != null ? val.toString() : "";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public int compareDates(String dateOne, String dateTwo) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            Date d1 = sdf.parse(dateOne);
            Date d2 = sdf.parse(dateTwo);
            return d1.compareTo(d2);
        } catch (Exception e) {
            e.printStackTrace();
            return -10;
        }
    }

    public String changeDataFormat(String date, String fromFormat, String toFormat) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(fromFormat);
            Date parsedDate = sdf.parse(date);
            sdf = new SimpleDateFormat(toFormat);
            return sdf.format(parsedDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }

    public Date getDateFromDatePicker(String pickerDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MM-yyyy");
            return inputFormat.parse(pickerDate);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public int differenceOfDates(Date dateOne, Date dateTwo) {
        try {
            Calendar cal1 = Calendar.getInstance();
            Calendar cal2 = Calendar.getInstance();
            cal1.setTime(dateOne);
            cal2.setTime(dateTwo);

            return 1 + daysBetween(cal1.getTime(), cal2.getTime());
        } catch (Exception e) {
            e.printStackTrace();
            return -10;
        }
    }

    public int daysBetween(Date d1, Date d2) {
        long diff = d2.getTime() - d1.getTime();
        return (int) (diff / (1000 * 60 * 60 * 24));
    }
}
