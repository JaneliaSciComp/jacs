
package org.janelia.it.jacs.compute.service.prokAnnotation;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:47:08 PM
 */
public class SkewUpdateService extends ProkAnnotationBaseService {

    public String getCommandLine() {
        return "skew_update.dbi -D " + _targetDatabase + " -u " + _databaseUser + " -p " + _databasePassword + " -I";
    }

    @Override
    protected String getSGEQueue() {
        return "-l fast";
    }

}
