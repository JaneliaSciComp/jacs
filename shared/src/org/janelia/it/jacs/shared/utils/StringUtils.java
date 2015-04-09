package org.janelia.it.jacs.shared.utils;

import java.util.Arrays;
import java.util.Collection;

import org.apache.log4j.Logger;

/**
 * Some helpful utilities for strings.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class StringUtils {
	public static boolean isEmpty(String s) {
		return s==null || "".equals(s);
	}

	public static boolean areAllEmpty(Collection<String> strings) {
	    for (String s : strings) {
	        if (!isEmpty(s)) {
	            return false;
	        }
	    }
	    return true;
    }

	public static String getIndent(int level, String single) {
        StringBuilder indent = new StringBuilder();
        for(int i=0; i<level; i++) {
            indent.append(single);
        }
        return indent.toString();
	}
	
	public static String underscoreToTitleCase(String name) {
    	String[] words = name.split("_");
    	StringBuffer buf = new StringBuffer();
    	for(String word : words) {
    		char c = Character.toUpperCase(word.charAt(0));
    		if (buf.length()>0) buf.append(' ');
    		buf.append(c);
    		buf.append(word.substring(1).toLowerCase());
    	}
    	return buf.toString();
    }

	public static String getCommaDelimited(Object... objArray) {
		return getCommaDelimited(Arrays.asList(objArray));
	}
	
	public static String getCommaDelimited(Collection<?> objs) {
		return getCommaDelimited(objs, null);
	}
	
	public static String getCommaDelimited(Collection<?> objs, Integer maxLength) {
		if (objs==null) return null;
		StringBuffer buf = new StringBuffer();
		for(Object obj : objs) {
			if (maxLength!=null && buf.length()+3>=maxLength) {
				buf.append("...");
				break;
			}
			if (buf.length()>0) buf.append(", ");
			buf.append(obj.toString());
		}
		return buf.toString();
	}

    public static String defaultIfNullOrEmpty(String o, String defaultString) {
        if (isEmpty(o)) return defaultString;
        return o.toString();
    }
    
    public static String defaultIfNull(Object o, String defaultString) {
        if (o==null) return defaultString;
        return o.toString();
    }

	public static String emptyIfNull(Object o) {
	    if (o==null) return "";
	    return o.toString();
	}

    /** Prototype color: 91 121 227 must be turned into a 6-digit hex representation. */
    public static String encodeToHex(String colors, Logger logger) {
        StringBuilder builder = new StringBuilder();
        String[] colorArr = colors.trim().split(" ");
        if ( colorArr.length != 3 ) {
            logger.warn("Color parse did not yield three values.  Leaving all-red : " + colors);
        }
        else {
            for ( int i = 0; i < colorArr.length; i++ ) {
                try {
                    String hexStr = Integer.toHexString(Integer.parseInt(colorArr[i])).toUpperCase();
                    if ( hexStr.length() == 1 ) {
                        hexStr = "0" + hexStr;
                    }
                    if ( hexStr.length() == 1 ) {
                        hexStr = "0" + hexStr;
                    }
                    builder.append( hexStr );
                } catch ( NumberFormatException nfe ) {
                    logger.warn("Failed to parse " + colorArr[i] + " as Int.  Leaving 80.");
                    builder.append( "80" );
                }
            }
        }
        return builder.toString();
    }

    // Borrowed from Apache Commons
    public static int countMatches(final String str, final String sub) {
        if (isEmpty(str) || isEmpty(sub)) {
            return 0;
        }
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }   
}
