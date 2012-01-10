
package org.janelia.it.jacs.compute.service.prokAnnotation;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:47:56 PM
 */
public class TerminatorsFinderService extends ProkAnnotationBaseService {

    public String getCommandLine() {
        return "terminators_finder.dbi -D " + _targetDatabase + " -u " + _databaseUser + " -p " + _databasePassword + " -g";
    }

    @Override
    protected String getSGEQueue() {
        return "-l fast";
    }

}
