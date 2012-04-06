package org.janelia.it.jacs.compute.api.support;


import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utilities used by the server, which are likewise useful in the client.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SolrUtils {

	public enum DocType {
		ENTITY,
		SAGE_TERM
	}
	
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
	 * Format the given name in lowercase, with underscores instead of spaces. For example, "Tiling Pattern" -> "tiling_pattern"
	 * @param name
	 * @return
	 */
    public static String getFormattedName(String name) {
    	return name.toLowerCase().replaceAll("\\s+", "_");
    }
    
	/**
	 * Get the SOLR field name from an attribute name. For example, "Tiling Pattern" -> "tiling_pattern_txt"
	 * @param name
	 * @return
	 */
    public static String getDynamicFieldName(String name) {
    	return getFormattedName(name)+"_txt";
    }
    
    /**
     * Get the SOLR field name for a Sage CV term. For example, "effector" -> "effector_t"
     * @param term
     * @param sageType a data type from Sage, such as "float" or "integer" or "text"
     * @return
     */
    public static String getSageFieldName(String term, SageTerm sageTerm) {
		String solrSuffix = "_t"; // default to text-based indexing
		String sageType = sageTerm.getDataType();
		if (sageType != null) {
			if ("float".equals(sageType)) {
				solrSuffix = "_d";
			}
			else if ("integer".equals(sageType)) {
				solrSuffix = "_l";		
			}
		}
		return term+solrSuffix;
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
