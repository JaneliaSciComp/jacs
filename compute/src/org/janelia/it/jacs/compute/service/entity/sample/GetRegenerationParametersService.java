package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.compute.service.align.AlignmentInputFile;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Prepare the pipeline for regeneration subtask submission.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetRegenerationParametersService extends AbstractEntityService {
	
    public void execute() throws Exception {

    	Long sampleId = data.getRequiredItemAsLong("SAMPLE_ENTITY_ID");
    	Entity parentEntity = entityBean.getAncestorWithType(ownerKey, sampleId, EntityConstants.TYPE_SAMPLE);
    	Long parentSampleId = parentEntity==null ? sampleId : parentEntity.getId();
		data.putItem("REGENERATE_SAMPLE_ID", parentSampleId);
		
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
