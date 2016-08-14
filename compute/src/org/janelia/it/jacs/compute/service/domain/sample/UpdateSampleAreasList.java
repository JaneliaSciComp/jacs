package org.janelia.it.jacs.compute.service.domain.sample;

import java.util.List;

import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.domain.model.AnatomicalArea;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * This is a service that deals with a peculiar issues with the process engine: when an object is updated, it's only
 * updated in one reference. In this case, we updated SAMPLE_AREA, but the list its part of, SAMPLE_AREAS, does not
 * get updated. This service just updates the list.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class UpdateSampleAreasList extends AbstractDomainService {

    public void execute() throws Exception {

        List<AnatomicalArea> sampleAreas = (List<AnatomicalArea>) data.getItem("SAMPLE_AREAS");
        AnatomicalArea updatedSampleArea = (AnatomicalArea) data.getItem("SAMPLE_AREA");

        for(int i=0; i<sampleAreas.size(); i++) {
            AnatomicalArea aa = sampleAreas.get(i);
            if (StringUtils.areEqual(aa.getName(), updatedSampleArea.getName())) {
                sampleAreas.set(i, updatedSampleArea);
            }
        }

        data.putItem("SAMPLE_AREAS", sampleAreas);
    }
}
