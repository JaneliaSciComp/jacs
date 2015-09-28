package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.HashSet;
import java.util.Set;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * If LSMs don't have a chan spec, we need to infer it from the dye spec and populate it so that the rest of the pipeline can use it.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InferChannelSpecificationService extends AbstractEntityService {

    private String channelDyeSpec;
    private Set<String> referenceDyes = new HashSet<>();
    
    public void execute() throws Exception {

        this.channelDyeSpec = data.getItemAsString("CHANNEL_DYE_SPEC");
        
        if (channelDyeSpec!=null) {
            String[] channels = channelDyeSpec.split(";");
            for(String channel : channels) {
                String[] parts = channel.split("=");
                String channelTag = parts[0];
                String[] channelDyes = parts[1].split(",");
                if ("reference".equals(channelTag)) {
                    for(String dye : channelDyes) {
                        referenceDyes.add(dye);
                    }
                }
            }
            if (referenceDyes.isEmpty()) {
                throw new IllegalStateException("No reference dye defined in dye spec: "+channelDyeSpec);
            }
        }
        
        AnatomicalArea sampleArea = (AnatomicalArea) data.getRequiredItem("SAMPLE_AREA");
        
        for(MergedLsmPair mergedLsmPair : sampleArea.getMergedLsmPairs()) {
            ensureChanSpec(mergedLsmPair.getLsmEntityId1());
            ensureChanSpec(mergedLsmPair.getLsmEntityId2());
        }
    }
    
    private void ensureChanSpec(Long lsmId) throws Exception {

        if (lsmId==null) return;
        Entity lsm = entityBean.getEntityById(lsmId);
        String chanSpec = lsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
        String channelDyeNames = lsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_DYE_NAMES);
        
        if (chanSpec==null) {
            if (referenceDyes.isEmpty() || channelDyeNames==null) {
                throw new IllegalStateException("No channel spec or dye spec defined for LSM with id="+lsmId);
            }
            else {
                StringBuilder chanSpecSb = new StringBuilder();
                for(String dye : channelDyeNames.split(",")) {
                    if (referenceDyes.contains(dye)) {
                        chanSpecSb.append("r");
                    }
                    else {
                        chanSpecSb.append("s");
                    }
                }
                contextLogger.info("Chanspec was inferred as "+chanSpecSb+" for LSM id="+lsmId);
                entityHelper.setChannelSpec(lsm, chanSpecSb.toString());
            }
        }
        else {
            contextLogger.info("Chanspec was provided as "+chanSpec+" for LSM id="+lsmId);
        }
    }
}
