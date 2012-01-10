
package org.janelia.it.jacs.compute.service.prokAnnotation;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:44:42 PM
 */
public class ParseForNcRnaService extends ProkAnnotationBaseService {

    public String getCommandLine() {
        return "parse_for_ncRNAs_controller.pl -D " + _targetDatabase + " -u " + _databaseUser + " -p " + _databasePassword + " -i";
    }

}
