package org.janelia.it.jacs.compute.util;

import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * Utility methods for dealing with JFS Paths and Webdav URLS.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class JFSUtils {

    public static final String JFS_PREFIX_URL =
            SystemConfigurationProperties.getString("JFS.URL");

    public static final String JFS_LSM_STORE =
            SystemConfigurationProperties.getString("JFS.LSMStore");

    public static final String JFS_PBD_STORE =
            SystemConfigurationProperties.getString("JFS.PBDStore");
    
    public static final String JFS_SCALITY_NAMESPACE =
            SystemConfigurationProperties.getString("JFS.Scality.Namespace");

    public static String getWebdavUrlForJFSPath(String jfsPath) {
    	if (jfsPath==null) return null;
        return JFS_PREFIX_URL+jfsPath;
    }

    // TODO: remove this once the entity services go away
    public static String getScalityPathFromEntity(Entity entity) {
        throw new UnsupportedOperationException("No longer supported. Use the new ScalitEntity method.");
    }
    
    public static String getScalityPathFromEntity(ScalityEntity entity) {
        String filepath = entity.getFilepath();
        if (filepath.startsWith(entity.getStore())) {
            return filepath;
        }
        // The entity doesn't know it's in JFS yet, so we need to create the path from scratch 
        StringBuilder sb = new StringBuilder();

        if (JFS_PBD_STORE.equals(entity.getStore()) && entity.getName().endsWith(".v3dpbd")) {
        	sb.append(JFS_PBD_STORE);
            sb.append("/");
            sb.append(JFS_SCALITY_NAMESPACE);
            sb.append("/");
            sb.append(entity.getId());
            sb.append("/");
            sb.append(entity.getName());
        }
        else if (JFS_LSM_STORE.equals(entity.getStore())) {
        	filepath = filepath.substring(1);
        	filepath = filepath.substring(filepath.indexOf('/'));
        	sb.append(JFS_LSM_STORE);
            sb.append("/");
            sb.append(JFS_SCALITY_NAMESPACE);
            sb.append(filepath);
        }
        else {
        	throw new IllegalArgumentException("No filestore defined for "+entity.getStore());
        }
        
        return sb.toString();
    }
}
