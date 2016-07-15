package org.janelia.it.jacs.compute.service.domain.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.compute.service.align.AlignmentInputFile;
import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Prepare the pipeline for regeneration subtask submission.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetRegenerationParametersService extends AbstractDomainService {
	
    public void execute() throws Exception {
		
    	List<AlignmentInputFile> alignmentInputFiles = (List<AlignmentInputFile>)data.getRequiredItem("ALIGNMENT_INPUTS");
        
    	Set<String> objectives = new HashSet<>();
		for (AlignmentInputFile alignmentInputFile : alignmentInputFiles) {
			objectives.add(alignmentInputFile.getObjective());
		}

		List<String> objectiveList = new ArrayList<>(objectives);
		Collections.sort(objectiveList);
		
		data.putItem("REGENERATE_OBJECTIVES", Task.csvStringFromCollection(objectiveList));
    }
}
