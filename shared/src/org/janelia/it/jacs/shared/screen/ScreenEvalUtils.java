package org.janelia.it.jacs.shared.screen;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * Utility methods for dealing with Arnim's screen evaluation hierarchy.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ScreenEvalUtils {

	public static int getValueFromFolderName(Entity entity) {
		return getValueFromFolderName(entity.getName());
	}
	
	public static int getValueFromFolderName(String folderName) {
		return Integer.parseInt(""+folderName.charAt(folderName.length()-1));
	}
	
	public static int getValueFromAnnotation(String annotationValue) {
		return Integer.parseInt(""+annotationValue.charAt(1));
	}
	
	public String getKey(Entity compartmentEntity, Entity intEntity, Entity distEntity) {
		int i = getValueFromFolderName(intEntity);
		int d = getValueFromFolderName(distEntity);
		return getKey(compartmentEntity.getName(),i,d);
	}
	
	public static String getKey(String compartment, int i, int d) {
		return compartment+"/"+i+"/"+d;
	}


	public static Integer getIntensityValueFromKey(String key) {
		try {
			String[] parts = key.split("/");
			return Integer.parseInt(parts[1]);	
		}
		catch (Exception e) {
			return null;
		}
	}

	public static Integer getDistributionValueFromKey(String key) {
		try {
			String[] parts = key.split("/");
			return Integer.parseInt(parts[2]);	
		}
		catch (Exception e) {
			return null;
		}
	}
}
