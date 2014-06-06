package org.janelia.it.workstation.shared.filestore;

import org.hibernate.Hibernate;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionModel;
import org.janelia.it.workstation.shared.util.ConsoleProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.janelia.it.workstation.web.FileProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import org.janelia.it.workstation.api.entity_model.management.ModelMgrUtils;

/**
 * Translate between paths to various mounted file resources.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PathTranslator {

    private static final Logger log = LoggerFactory.getLogger(PathTranslator.class);

    public static final String JACS_DATA_PATH_MAC = ConsoleProperties.getString("remote.defaultMacPath");
    public static final String JACS_DATA_PATH_NFS = ConsoleProperties.getString("remote.defaultLinuxDataPath");
    public static final String JACS_DEPLOYMENT_PATH_NFS = ConsoleProperties.getString("remote.defaultLinuxDeploymentPath");
    public static final String JACS_DATA_PATH_WINDOWS = ConsoleProperties.getString("remote.defaultWindowsPath");
    public static final String JACS_DATA_MOUNT_MAC = ConsoleProperties.getString("remote.remoteMacMount");
    public static final String JACS_DATA_MOUNT_WINDOWS = ConsoleProperties.getString("remote.remoteWindowsMount");

    public static String jacsDataPath;

    public static void initFromModelProperties(SessionModel sessionModel) {

        jacsDataPath = (String) sessionModel.getModelProperty(SessionMgr.JACS_DATA_PATH_PROPERTY);
        if (jacsDataPath == null) {
            File jacsData = new File(PathTranslator.JACS_DATA_PATH_NFS);
            if (jacsData.canRead()) {
                jacsDataPath = jacsData.getAbsolutePath();
            }
            else {
                jacsDataPath = PathTranslator.getOsSpecificRootPath();
            }
            sessionModel.setModelProperty(SessionMgr.JACS_DATA_PATH_PROPERTY, jacsDataPath);
        }

        log.info("Using JACS data path: " + jacsDataPath);
    }

    /**
     * Converts the given path to the current platform.
     *
     * @param filepath original path to the item
     * @return returns the most appropriate path to the file based on mount type and OS
     */
    public static String convertPath(String filepath) {

        if (!jacsDataPath.startsWith(JACS_DATA_PATH_NFS)) {
            filepath = filepath.replace(JACS_DATA_PATH_NFS, jacsDataPath);
        }

        if (SystemInfo.isWindows) {
            filepath = filepath.replaceAll("/", "\\\\");
        }

        return filepath;
    }

    /**
     * Modify the given entity tree so that any file path attributes are appropriate for the current platform.
     */
    public static Entity translatePathsToCurrentPlatform(Entity entity) {

        for (EntityData entityData : ModelMgrUtils.getAccessibleEntityDatas(entity)) {
            if (entityData.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_FILE_PATH)) {
                entityData.setValue(convertPath(entityData.getValue()));
            }
            else {
                Entity child = entityData.getChildEntity();
                if (child != null && Hibernate.isInitialized(child)) {
                    translatePathsToCurrentPlatform(child);
                }
            }
        }

        return entity;
    }

    /**
     * Modify the given entity tree so that any file path attributes are proxied by our local file service.
     */
    public static Entity translatePathsToProxy(Entity entity) {

        for (EntityData entityData : ModelMgrUtils.getAccessibleEntityDatas(entity)) {
            if (entityData.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_FILE_PATH)) {
                String path = entityData.getValue();
                try {
                    String url = FileProxyService.getProxiedFileUrl(path).toString();
                    entityData.setValue(url);
                }
                catch (MalformedURLException e) {
                    log.error("Error translating path to proxy: " + path, e);
                }
            }
            else {
                Entity child = entityData.getChildEntity();
                if (child != null && Hibernate.isInitialized(child)) {
                    translatePathsToCurrentPlatform(child);
                }
            }
        }

        return entity;
    }

    private static String getOsSpecificRootPath() {
        if (SystemInfo.isMac) {
            return PathTranslator.JACS_DATA_PATH_MAC;
        }
        else if (SystemInfo.isLinux) {
            return PathTranslator.JACS_DATA_PATH_NFS;
        }
        else if (SystemInfo.isWindows) {
            return PathTranslator.JACS_DATA_PATH_WINDOWS;
        }
        return "";
    }
}
