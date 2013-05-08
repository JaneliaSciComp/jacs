package org.janelia.it.jacs.shared.utils;

import org.apache.log4j.Logger;

import java.util.Collection;

/**
 * Some helpful utilities for strings.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class StringUtils {
	public static boolean isEmpty(String s) {
		return s==null || "".equals(s);
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

	public static String getCommaDelimited(Collection objs, int maxLength) {
		if (objs==null) return null;
		StringBuffer buf = new StringBuffer();
		for(Object obj : objs) {
			if (buf.length()+3>=maxLength) {
				buf.append("...");
				break;
			}
			if (buf.length()>0) buf.append(", ");
			buf.append(obj.toString());
		}
		return buf.toString();
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

}
