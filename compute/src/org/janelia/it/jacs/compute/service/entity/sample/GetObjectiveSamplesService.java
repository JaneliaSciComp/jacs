package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.List;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.cv.Objective;
import org.janelia.it.jacs.shared.utils.EntityUtils;


/**
 * Extracts sub-samples based on their objective.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetObjectiveSamplesService extends AbstractEntityService {

    public void execute() throws Exception {

        final Entity sampleEntity = entityHelper.getRequiredSampleEntity(data);
        populateChildren(sampleEntity);
        final List<Entity> subSamples = EntityUtils.getChildrenOfType(sampleEntity, EntityConstants.TYPE_SAMPLE);
        
        if (! subSamples.isEmpty()) {

            data.putItem("PARENT_SAMPLE_ID", sampleEntity.getId().toString());

            String objective;
            String subSampleId;
            for (Entity subSample : subSamples) {
                objective = subSample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
                subSampleId = subSample.getId().toString();
                if (Objective.OBJECTIVE_20X.getName().equals(objective)) {
                    data.putItem("SAMPLE_20X_ID", subSampleId);
                } else if (Objective.OBJECTIVE_40X.getName().equals(objective)) {
                    data.putItem("SAMPLE_40X_ID", subSampleId);
                } else if (Objective.OBJECTIVE_63X.getName().equals(objective)) {
                    data.putItem("SAMPLE_63X_ID", subSampleId);
                }
            }

        } else {

            contextLogger.info("No subSamples found");
            String objective = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
            if (objective != null) {
                data.putItem("SAMPLE_" + objective.toUpperCase() + "_ID", sampleEntity.getId().toString());
            }

        }
    }
}
