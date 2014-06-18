package org.janelia.it.jacs.compute.service.validation;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;

/**
 * This "service" is not a fait acompli.  It may disappear, be renamed, moved, or otherwise refactored.  It is
 * mainly for practice.
 *
 * Created by fosterl on 6/17/14.
 */
@SuppressWarnings("unused")
public class SimpleValidationService implements IService {
    private Logger logger = Logger.getLogger(SimpleValidationService.class);

    @Override
    public void execute(IProcessData processData) throws ServiceException {
        logger.info("Validated:  I've been executed.");
    }
}
