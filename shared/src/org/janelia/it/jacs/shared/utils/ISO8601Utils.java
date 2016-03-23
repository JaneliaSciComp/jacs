package org.janelia.it.jacs.shared.utils;

import java.util.Date;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Tools for dealing with ISO8601 dates.
 *  
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ISO8601Utils {

    private static final Logger log = Logger.getLogger(ISO8601Utils.class);
    
    private static final DateTimeFormatter parser = ISODateTimeFormat.dateTimeNoMillis();
    
    public static Date parse(String dateTimeStr) {
        if (dateTimeStr==null) return null;
        try {
            return parser.parseDateTime(dateTimeStr).toDate();
        }
        catch (Exception e) {
            log.error("Cannot parse ISO8601 date: "+dateTimeStr,e);
            return null;
        }
    }

    public static String format(Date date) {
        if (date==null) return null;
        return parser.print(new DateTime(date));
    }
}
