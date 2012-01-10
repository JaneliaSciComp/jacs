
package org.janelia.it.jacs.compute.service.prokAnnotation;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.tasks.prokAnnotation.PressDbTask;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:45:04 PM
 */
public class PressDbService extends ProkAnnotationBaseService {

    public String getCommandLine() throws ServiceException {
        String prokDirPath = this._targetDirectory;
        File prokDir = new File(prokDirPath);
        File targetFile = null;
        if (prokDir.exists() && prokDir.isDirectory()) {
            File[] tmpFiles = prokDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(task.getParameter(PressDbTask.PARAM_FILE_SUFFIX));
                }
            });
            if (null != tmpFiles && tmpFiles.length > 0) {
                targetFile = tmpFiles[0];
            }
        }
        if (null != targetFile && targetFile.exists() &&
                null != task.getParameter(PressDbTask.PARAM_FILE_SUFFIX) && !"".equals(task.getParameter(PressDbTask.PARAM_FILE_SUFFIX))) {
            return "pressdb " + targetFile.getAbsolutePath();
        }
        else {
            throw new ServiceException("Execution of " + getServiceName() + " failed because the file moniker or suffix was undefined.");
        }
    }

}
