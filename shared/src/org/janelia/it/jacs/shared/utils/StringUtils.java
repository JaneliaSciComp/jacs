package org.janelia.it.jacs.shared.utils;

/**
 * Some helpful utilities for strings.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class StringUtils {

	public static boolean isEmpty(String s) {
		return s==null || "".equals(s);
	}
	
}
