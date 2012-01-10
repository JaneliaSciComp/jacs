
package org.janelia.it.jacs.compute.service.prokAnnotation;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:43:05 PM
 */
public class MolecularWeightUpdateService extends ProkAnnotationBaseService {

    public String getCommandLine() {
        return "molwt_update.dbi -D " + _targetDatabase + " -u " + _databaseUser + " -p " + _databasePassword + " -I";
    }

    @Override
    protected String getSGEQueue() {
        return "-l fast";
    }

}
