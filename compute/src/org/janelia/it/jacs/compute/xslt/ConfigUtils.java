package org.janelia.it.jacs.compute.xslt;

/**
 * Created by Leslie L Foster on 8/28/2016.
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utilities to help with configuration.
 *
 * @author Leslie L Foster
 */
public class ConfigUtils {

    /**
     * Use this to turn a filename into a properties collection.
     *
     * @param file relative path to properties file.  No drive letter, should be
     *             in class path, but otherwise fully-qualified down to extension.
     * @return all props from file.
     */
    public static Properties getProperties(String file) {
        InputStream inStream = null;
        Properties properties = new Properties();
        try {
            inStream = ConfigUtils.class.getClassLoader().getResourceAsStream(file);
            properties.load(inStream);
        } catch (Exception ex) {
            // Do nothing.  Fail silently.  The properties are optional.
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException ioe2) {
                    // Nada.
                }
            }
        }
        return properties;
    }
}

