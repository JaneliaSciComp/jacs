
package org.janelia.it.jacs.compute.service.prokAnnotation;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:46:53 PM
 */
public class SignalPUpdateService extends ProkAnnotationBaseService {

    public String getCommandLine() {
        return "signalP_update.dbi -D " + _targetDatabase + " -u " + _databaseUser + " -p " + _databasePassword + " -I";
    }

}
