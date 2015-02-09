package org.janelia.it.jacs.shared.solr;


import org.janelia.it.jacs.shared.utils.StringUtils;

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
	 * Format the given name in lowercase, with underscores instead of spaces. For example, "Tiling Pattern" -> "tiling_pattern"
	 * @param name
	 * @return
	 */
    public static String getFormattedName(String name) {
    	return name.toLowerCase().replaceAll("\\W+", "_");
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
     * Get the SOLR field name from an attribute name. For example, "Tiling Pattern" -> "tiling_pattern_txt"
     * @param name
     * @return
     */
    public static String getAttributeNameFromSolrFieldName(String solrFieldName) {
        if (solrFieldName==null) return null;
        String name = solrFieldName.replaceFirst("_txt", "");
        return StringUtils.underscoreToTitleCase(name);
    }
    
    /**
     * Get the SOLR field name for a Sage CV term. For example, "effector" -> "sage_effector_t"
     * @param term
     * @param sageType a data type from Sage, such as "float" or "integer" or "text" or "date_time"
     * @return
     */
    public static String getSageFieldName(SageTerm sageTerm) {
    	String sageType = sageTerm.getDataType();
		String solrSuffix = "_t"; // default to text-based indexing
		if (sageType != null) {
			if ("float".equals(sageType)) {
				solrSuffix = "_d";
			}
			else if ("date".equals(sageType) || "date_time".equals(sageType)) {
				solrSuffix = "_dt";
			}
			else if ("boolean".equals(sageType)) {
				solrSuffix = "_b";
			}
			else if ("integer".equals(sageType)) {
				solrSuffix = "_l";		
			}
		}
		return "sage_"+sageTerm.getKey()+solrSuffix;
    }
}
