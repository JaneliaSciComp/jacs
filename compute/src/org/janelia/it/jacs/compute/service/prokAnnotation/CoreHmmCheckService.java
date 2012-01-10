
package org.janelia.it.jacs.compute.service.prokAnnotation;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:41:37 PM
 */
public class CoreHmmCheckService extends ProkAnnotationBaseService {

    public String getCommandLine() {
        return "core_HMM_check.pl -db " + _targetDatabase + " -user " + _databaseUser + " -password " + _databasePassword;
    }

    @Override
    protected String getSGEQueue() {
        return "-l fast";
    }

}
