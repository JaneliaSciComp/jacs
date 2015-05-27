package org.janelia.it.jacs.compute.service.common;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.tasks.Task;


/**
 * Initializes the given output variable with a list consisting of the individual strings in an
 * input comma-separated value list string. 
 *   INPUT_CSV
 *   OUTPUT_VAR
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ParseCSVStringService implements IService {

    public void execute(IProcessData processData) throws ServiceException {
        try {
            String csv = (String)processData.getItem("INPUT_CSV");
            String outputVarName = (String)processData.getItem("OUTPUT_VAR");
            processData.putItem(outputVarName, Task.listOfStringsFromCsvString(csv));
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
