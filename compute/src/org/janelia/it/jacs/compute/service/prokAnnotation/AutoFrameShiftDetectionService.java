
package org.janelia.it.jacs.compute.service.prokAnnotation;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:38:50 PM
 */
public class AutoFrameShiftDetectionService extends ProkAnnotationBaseService {

    public String getCommandLine() {
        return "auto_fs_detection.dbi -D " + _targetDatabase + " -u " + _databaseUser + " -p " + _databasePassword + " -I";
    }

}
