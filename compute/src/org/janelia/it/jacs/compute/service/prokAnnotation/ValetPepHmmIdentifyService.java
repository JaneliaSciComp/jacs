
package org.janelia.it.jacs.compute.service.prokAnnotation;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:48:38 PM
 */
public class ValetPepHmmIdentifyService extends ProkAnnotationBaseService {

    public String getCommandLine() {
        return "valet_pep_hmm_ident.pl -D " + _targetDatabase + " -u " + _databaseUser + " -p " + _databasePassword + " -x";
    }

}
