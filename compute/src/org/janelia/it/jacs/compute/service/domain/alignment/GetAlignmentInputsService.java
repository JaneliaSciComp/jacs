package org.janelia.it.jacs.compute.service.domain.alignment;

import java.lang.reflect.Constructor;

import org.janelia.it.jacs.compute.service.align.Aligner;
import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;

/**
 * Decides what analysis pipeline to run, based on the enumerated value.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetAlignmentInputsService extends AbstractDomainService {

	@Override 
    public void execute() throws Exception {
	    Aligner aligner = getAlignerService();
	    contextLogger.info("Using aligner to populate input variables: "+aligner.getClass().getName());
	    aligner.populateInputVariables(processData);
    }
	
    private Aligner getAlignerService() throws Exception {

        String iServiceName = processData.getString("iservice");
        contextLogger.debug("Instantiating the class " + iServiceName);

        Class[] paramTypes = {};
        Constructor constructor = Class.forName(iServiceName).getConstructor(paramTypes);

        // Instantiate default constructor
        return(Aligner) constructor.newInstance();
    }
}
