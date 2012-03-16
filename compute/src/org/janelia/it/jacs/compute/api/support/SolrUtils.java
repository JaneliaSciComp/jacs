package org.janelia.it.jacs.compute.api.support;


import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utilities used by the server, which are likewise useful in the client.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SolrUtils {
	
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    /**
     * Format the given date according to "Complete ISO 8601 Date" format supported by SOLR query syntax.
     * @param date
     * @return
     */
    public static String formatDate(Date date) {
    	return dateFormat.format(date);
    }
    
	/**
	 * Get the SOLR field name from an attribute name. For example, "Tiling Pattern" -> "tiling_pattern_txt"
	 * @param name
	 * @return
	 */
    public static String getFieldName(String name) {
    	return name.toLowerCase().replaceAll("\\s+", "_")+"_txt";
    }
    
    /**
     * Returns the formatted annotation tag for the given key/value annotation pair.
     * @param key
     * @param value
     * @return
     */
    public static String getAnnotationTag(String key, String value) {
    	return (value == null) ? key : key + " = " + value;
    }
}
