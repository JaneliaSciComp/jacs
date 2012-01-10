
package org.janelia.it.jacs.compute.service.prokAnnotation;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:40:09 PM
 */
public class BuildCoordinateFileService extends ProkAnnotationBaseService {

    public String getCommandLine() {
        return "build_coordset.dbi -D " + _targetDatabase + " -u " + _databaseUser + " -p " + _databasePassword + " -r " + _targetDirectory;
    }

    @Override
    protected String getSGEQueue() {
        return "-l fast";
    }

}
