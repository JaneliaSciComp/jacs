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
    
    /** Given some string starting with an integer, find next pos after. */
    public static int findFirstNonDigitPosition(String inline) {
        int afterDigits = 0;
        while (Character.isDigit(inline.charAt(afterDigits))) {
            afterDigits++;
        }
        return afterDigits;
    }

    /** Given some string ending with an integer, find where the int begins. */
    public static int lastDigitPosition(String inline) {
        int beforeDigits = inline.length() - 1;
        while (Character.isDigit(inline.charAt(beforeDigits))) {
            beforeDigits--;
        }
        beforeDigits ++; // Move back to a convenient position.
        return beforeDigits;
    }

    /**
     * Given some filename, return an 'iterated' version, containing a counter
     * offset.  In this fashion, 'sub names' iterated over a count can be 
     * generated from a 'parent name'.
     * 
     * Example: mytext.txt -> mytext_1.txt   OR   mytext_2.txt
     * 
     * @param baseFileName make a variant of this
     * @param offset use this offset in the new variant
     * @return the iterated filename.
     */
    public static String getIteratedName(final String baseFileName, int offset) {
        String newName = baseFileName;
        int periodPos = newName.indexOf('.');
        if (periodPos > -1) {
            newName = newName.substring(0, periodPos)
                    + '_' + offset + newName.substring(periodPos);
        }
        return newName;
    }

    
}
