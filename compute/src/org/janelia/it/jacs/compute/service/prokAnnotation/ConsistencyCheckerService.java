
package org.janelia.it.jacs.compute.service.prokAnnotation;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:39:52 PM
 */
public class ConsistencyCheckerService extends ProkAnnotationBaseService {

    public String getCommandLine() {
        return "consistency_check/consistency_checks.cgi  -d "+_targetDatabase+" -u "+_databaseUser+" -p " + _databasePassword;
    }

    @Override
    protected String getSGEQueue() {
        return "-l medium";
    }

}