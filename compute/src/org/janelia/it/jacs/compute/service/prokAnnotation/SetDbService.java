
package org.janelia.it.jacs.compute.service.prokAnnotation;

import org.janelia.it.jacs.compute.engine.service.ServiceException;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:45:52 PM
 */
public class SetDbService extends ProkAnnotationBaseService {

    public String getCommandLine() throws ServiceException {
        String prokDirPath = this._targetDirectory;
        File prokDir = new File(prokDirPath);
        File targetFile = null;
        if (prokDir.exists() && prokDir.isDirectory()) {
            File[] tmpFiles = prokDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".pep");
                }
            });
            if (null != tmpFiles && tmpFiles.length > 0) {
                targetFile = tmpFiles[0];
            }
        }
        if (null != targetFile && targetFile.exists()) {
            return "setdb " + targetFile.getAbsolutePath();
        }
        else {
            throw new ServiceException("Execution of " + getServiceName() + " failed because the file moniker was undefined.");
        }
    }

}
