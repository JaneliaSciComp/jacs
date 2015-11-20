package org.janelia.it.jacs.compute.util;

import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Utility methods for dealing with JFS Paths and Webdav URLS.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class JFSUtils {

    private static final String JFS_PREFIX_URL =
            SystemConfigurationProperties.getString("JFS.URL");

    private static final String JFS_LSM_STORE =
            SystemConfigurationProperties.getString("JFS.LSMStore");

    private static final String JFS_PBD_STORE =
            SystemConfigurationProperties.getString("JFS.PBDStore");
    
    private static final String JFS_SCALITY_NAMESPACE =
            SystemConfigurationProperties.getString("JFS.Scality.Namespace");

    public static String getWebdavUrlForJFSPath(String jfsPath) {
    	if (jfsPath==null) return null;
        return JFS_PREFIX_URL+jfsPath;
    }
    
    public static String getJFSPathFromEntity(Entity entity) {
        String jfsPath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_JFS_PATH);
        if (jfsPath!=null) return jfsPath;
        // The entity doesn't know it's in JFS yet, so we need to create the path from scratch 
        StringBuilder sb = new StringBuilder();

        if (EntityConstants.TYPE_IMAGE_3D.equals(entity.getEntityTypeName()) && entity.getName().endsWith(".v3dpbd")) {
        	sb.append(JFS_PBD_STORE);
            sb.append("/");
            sb.append(JFS_SCALITY_NAMESPACE);
            sb.append("/");
            sb.append(entity.getId());
            sb.append("/");
            sb.append(entity.getName());
        }
        else if (EntityConstants.TYPE_LSM_STACK.equals(entity.getEntityTypeName())) {
        	String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        	filepath = filepath.substring(1);
        	filepath = filepath.substring(filepath.indexOf('/'));
        	sb.append(JFS_LSM_STORE);
            sb.append("/");
            sb.append(JFS_SCALITY_NAMESPACE);
            sb.append("/");
            sb.append(filepath);
        }
        else {
        	throw new IllegalArgumentException("No filestore defined for "+entity.getEntityTypeName());
        }
        
        return sb.toString();
    }
}
