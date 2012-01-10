
package org.janelia.it.jacs.compute.service.prokAnnotation;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:48:11 PM
 */
public class TransmembraneUpdateService extends ProkAnnotationBaseService {

    public String getCommandLine() {
        return "tm_update.dbi -D " + _targetDatabase + " -u " + _databaseUser + " -p " + _databasePassword + " -I";
    }

}
