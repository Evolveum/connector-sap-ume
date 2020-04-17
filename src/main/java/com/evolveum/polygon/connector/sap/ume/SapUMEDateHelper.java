package com.evolveum.polygon.connector.sap.ume;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Date;
import java.util.TimeZone;

import static org.identityconnectors.common.StringUtil.isBlank;

//The object Date is a united and linking element

public class SapUMEDateHelper {

    private static final Log LOG = Log.getLog(SapUMEDateHelper.class);

    private static final SimpleDateFormat umeFormat = new SimpleDateFormat("yyyyMMddHHmmss'Z'");
    private static final SimpleDateFormat confFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final String CURRENT_VALID_TIME = "<CURRENT_TIME>";

    public static ZonedDateTime convertDateToZDT(Date date) {
        return ZonedDateTime.ofInstant(date.toInstant(), getTimeZone().toZoneId());
    }

    public static Date convertZDTToDate(ZonedDateTime zdt) {
        return Date.from(zdt.toInstant());
    }
    public static Long convertDateToLong(Date date) {
        return date.getTime();
    }

    public static Date convertLongToDate(Long l) {
        return new Date(l);
    }

    public static String convertDateToUmeString(Date date, SapUMEConfiguration conf) {
        int offset = 0;
        if(isBlank(conf.getUmeOffset())) {
            offset = getTimeZone().getRawOffset();
        } else {
            int hourOffset = Integer.parseInt(conf.getUmeOffset());
            offset = hourOffset*3600000;
        }
        return umeFormat.format(new Date(date.getTime()-offset));
    }

    public static Date convertUmeStringToDate(String ume, SapUMEConfiguration conf)  {
        Date date = null;
        try {
            if (ume.length() == 15) {
                date = umeFormat.parse(ume);
            } else {
                String patern = umeFormat.toPattern();
                if (ume.length() > 15) {
                    String add = "";
                    for(int f=0;f<ume.length()-15;f++) {
                        add=add+"y";
                    }
                    patern = add+patern;
                } else { //<15
                    patern = patern.substring(15-ume.length());
                }
                LOG.warn("Unexpected input for date parsing: {0}",ume);
                try {
                    SimpleDateFormat umeFormatNew = new SimpleDateFormat(patern);
                    date = umeFormatNew.parse(ume);
                } catch (java.text.ParseException e) {
                    LOG.error("Error in parsing ume time : value="+ume,e);
                    throw new InvalidAttributeValueException("Error in parsing ume time : value="+ume, e);
                }
            }
            int offset = 0;
            if(isBlank(conf.getUmeOffset())) {
                offset = getTimeZone().getRawOffset();
            } else {
                int hourOffset = Integer.parseInt(conf.getUmeOffset());
                offset = hourOffset*3600000;
            }
            return new Date(date.getTime() + offset);
        } catch (ParseException e) {
            LOG.error(e,"Error in parsing ume time : value="+ume);
            throw new InvalidAttributeValueException("Error in parsing ume time : value="+ume, e);
        }
    }

    public static Date convertConfStringToDate(String ume)  {
        try {
            return confFormat.parse(ume);
        } catch (ParseException e) {
            throw new ConnectorException("Error in parsing conf time : value="+ume, e);
        }
    }

    public static Date getCurrentTime() {
        return new Date();
    }

    public static TimeZone getTimeZone() {
        return TimeZone.getDefault();
    }

    public static String parseValidTime(String time,SapUMEConfiguration conf) {
        if(!isBlank(time)) {
            if(time.equals(CURRENT_VALID_TIME)) {
                return convertDateToUmeString(getCurrentTime(),conf); //now
            } else {
                return convertDateToUmeString(convertConfStringToDate(time),conf);
            }
        } else {
            return null;
        }
    }

    public static Object parseUmeDateValue(Object value, SapUMEConfiguration conf) {
        if(value!=null) {
            if (value instanceof ZonedDateTime) {
                return convertDateToUmeString(convertZDTToDate((ZonedDateTime) value), conf);
            } else  if (value instanceof Long) {
                return  convertDateToUmeString(convertLongToDate((Long) value), conf);
            } else {
                throw new ConnectorException("Unknown date object type "+value.getClass().getName());
            }
        } else {
            return null;
        }
    }
}

