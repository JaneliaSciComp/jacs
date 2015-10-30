
package org.janelia.it.jacs.compute.service.prokAnnotation;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:42:41 PM
 */
public class LipoproteinUpdateService extends ProkAnnotationBaseService {

    public String getCommandLine() {
        return "lipoprotein_update.dbi -D " + _targetDatabase + " -u " + _databaseUser + " -p " + _databasePassword + " -I";
    }

    @Override
    protected String getSGEQueue() {
        return "-l fast";
    }

}
