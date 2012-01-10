
package org.janelia.it.jacs.shared.utils;

import org.apache.log4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class is required by JAXB to write out the publication dates in a format we like.  The
 * javax.xml.bind.DatatypeConverter that would be used by default would write out the time which is
 * not needed.
 * <p/>
 * Note:  This class is not meant to be used as a general date formater utility class
 *
 * @author Tareq Nabeel
 */
public class DateFormatter {

    static Logger logger = Logger.getLogger(DateFormatter.class.getName());

    public static String printDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd").format(date.getTime());
    }

    public static Date parseDate(String newDate) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            return format.parse(newDate);
        }
        catch (ParseException e) {
            logger.error("Error during parse of the date: " + newDate + "\n" + e.getMessage(), e);
        }
        return null;
    }
}
