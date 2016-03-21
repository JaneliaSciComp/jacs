package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.compute.service.entity.AbstractDomainService;
import org.janelia.it.jacs.compute.service.exceptions.MetadataException;
import org.janelia.it.jacs.compute.util.ChanSpecUtils;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleTile;

/**
 * Extracts stuff about the Sample from the entity model and loads it into simplified objects for use by other services.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitSampleAttributesService extends AbstractDomainService {

    private Sample sample;
    private ObjectiveSample objectiveSample;
    
    public void execute() throws Exception {

        this.sample = entityHelper.getRequiredSample(data);
        this.objectiveSample = entityHelper.getRequiredObjectiveSample(sample, data);
        final String chanSpec = objectiveSample.getChanSpec();
        final String signalChannels = ChanSpecUtils.getSignalChannelIndexes(chanSpec);
        data.putItem("SIGNAL_CHANNELS", signalChannels);
        final String referenceChannels = ChanSpecUtils.getReferenceChannelIndexes(chanSpec);
        data.putItem("REFERENCE_CHANNEL", referenceChannels);

        final List<AnatomicalArea> sampleAreas = getSampleAreas(objectiveSample);
        if (sampleAreas != null) {
            // Singular for the for loop
            data.putItem("SAMPLE_AREA", sampleAreas);
            // Plural for normal usage (e.g. in the alignment pipeline)
            data.putItem("SAMPLE_AREAS", sampleAreas);
        }
    }

    private List<AnatomicalArea> getSampleAreas(ObjectiveSample objectiveSample) throws Exception {

        List<AnatomicalArea> sampleAreas = null;

        Map<String,AnatomicalArea> areaMap = new HashMap<String,AnatomicalArea>();

        for(SampleTile sampleTile : objectiveSample.getTiles()) {
            String area = null;
            List<LSMImage> lsms = domainDao.getDomainObjectsAs(sampleTile.getLsmReferences(), LSMImage.class);
            for(LSMImage lsmImage : lsms) {
                String lsmArea = lsmImage.getAnatomicalArea();
                if (lsmArea==null) lsmArea = "";
                if (area == null) {
                    area = lsmArea;
                }
                else if (!area.equals(lsmArea)) {
                    throw new MetadataException("No consensus for area in tile '"+sampleTile.getName()+"' on sample "+sample.getName());
                }
            }
            AnatomicalArea anatomicalArea = areaMap.get(area);
            if (anatomicalArea==null) {
                anatomicalArea = new AnatomicalArea(sample.getId(), objectiveSample.getObjective(), area);
                areaMap.put(area, anatomicalArea);
            }
            anatomicalArea.addTileName(sampleTile.getName());
        }

        sampleAreas = new ArrayList<AnatomicalArea>(areaMap.values());

        // A bit of a hack... sort brains last so that they are the default 2d images later on
        Collections.sort(sampleAreas, new Comparator<AnatomicalArea>() {
            @Override
            public int compare(AnatomicalArea o1, AnatomicalArea o2) {
                if (o1.getName().equals(o2.getName())) {
                    return 0;
                }
                if (o1.getName().equalsIgnoreCase("Brain")) {
                    return 1;
                }
                if (o2.getName().equalsIgnoreCase("Brain")) {
                    return -1;
                }
                return o1.getName().compareTo(o1.getName());
            }
        });

        return sampleAreas;
    }

}
