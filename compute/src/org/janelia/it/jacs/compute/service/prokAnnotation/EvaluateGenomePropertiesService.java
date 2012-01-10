
package org.janelia.it.jacs.compute.service.prokAnnotation;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:42:02 PM
 */
public class EvaluateGenomePropertiesService extends ProkAnnotationBaseService {

    public String getCommandLine() {
        return "evaluate_props.pl -d " + _targetDatabase + " -a ALL --config " + basePath + "GenProp.ini";
    }

}
