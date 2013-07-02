package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.*;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Extracts stuff about the Sample from the entity model and loads it into simplified objects for use by other services.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitSampleAttributesService extends AbstractEntityService {

    public void execute() throws Exception {
        	        
    	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
    	if (sampleEntityId == null || "".equals(sampleEntityId)) {
    		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
    	}
    	
    	Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
    	if (sampleEntity == null) {
    		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
    	}
    	
    	if (!EntityConstants.TYPE_SAMPLE.equals(sampleEntity.getEntityType().getName())) {
    		throw new IllegalArgumentException("Entity is not a sample: "+sampleEntityId);
    	}
    	
    	logger.info("Retrieved sample: "+sampleEntity.getName()+" (id="+sampleEntityId+")");

        String chanSpec = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
        SampleHelper sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger);
        String signalChannels = sampleHelper.getSignalChannelIndexes(chanSpec);
        logger.info("Putting '"+signalChannels+"' in SIGNAL_CHANNELS");
        processData.putItem("SIGNAL_CHANNELS", signalChannels);
        String referenceChannels = sampleHelper.getReferenceChannelIndexes(chanSpec);
        logger.info("Putting '"+referenceChannels+"' in REFERENCE_CHANNEL");
        processData.putItem("REFERENCE_CHANNEL", referenceChannels);    
        
        populateChildren(sampleEntity);
        Entity supportingFiles = EntityUtils.getSupportingData(sampleEntity);

        // Parent samples may not have the rest of these attributes
        if (supportingFiles==null) return;
        
        supportingFiles = entityBean.getEntityTree(supportingFiles.getId());
        List<Entity> tileEntities = EntityUtils.getDescendantsOfType(supportingFiles, EntityConstants.TYPE_IMAGE_TILE, true);
        
        
        Map<String,AnatomicalArea> areaMap = new HashMap<String,AnatomicalArea>();
        
        for(Entity tileEntity : tileEntities) {
            String area = null;
            for(EntityData ed : tileEntity.getOrderedEntityData()) {
                Entity lsmStack = ed.getChildEntity();
                if (lsmStack != null && lsmStack.getEntityType().getName().equals(EntityConstants.TYPE_LSM_STACK)) {
                    String lsmArea = lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA);
                    if (lsmArea==null) lsmArea = "";
                    if (area == null) {
                        area = lsmArea;
                    }
                    else if (!area.equals(lsmArea)) {
                        throw new IllegalStateException("No consensus for area in tile '"+tileEntity.getName()+"' on sample "+sampleEntity.getName());
                    }
                }
            }
            AnatomicalArea anatomicalArea = areaMap.get(area);
            if (anatomicalArea==null) {
                anatomicalArea = new AnatomicalArea(area);
                areaMap.put(area, anatomicalArea);
            }
            anatomicalArea.addTile(tileEntity);
        }

        List<AnatomicalArea> sampleAreas = new ArrayList<AnatomicalArea>(areaMap.values());
        
        // A bit of a hack... sort brains last so that they are the default 2d images later on
        Collections.sort(sampleAreas, new Comparator<AnatomicalArea>()  {
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
        
        logger.info("Putting "+areaMap.values().size()+" values in SAMPLE_AREA");
        processData.putItem("SAMPLE_AREA", sampleAreas);
          
    }
}
