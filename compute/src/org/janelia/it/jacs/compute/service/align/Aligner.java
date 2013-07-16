package org.janelia.it.jacs.compute.service.align;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;

/**
 * An interface for all alignment pipelines so that they can be called in stages.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface Aligner {

    public void populateInputVariables(IProcessData processData) throws ServiceException;
}
