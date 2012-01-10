
package org.janelia.it.jacs.compute.service.prokAnnotation;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:41:16 PM
 */
public class ProkHmmer3SearchService extends ProkAnnotationBaseService {

    public String getCommandLine() {
        return "hmm3_search.dbi -D " + _targetDatabase + " -u " + _databaseUser + " -p " + _databasePassword +
                " -G " + getDefaultProjectCode();//+" -a "+_asmblId;// + " -I";
    }

}