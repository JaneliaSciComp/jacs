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

        final Entity sampleEntity = entityHelper.getRequiredSampleEntity(data);
        final String chanSpec = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
        final SampleHelper sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger);
        final String signalChannels = sampleHelper.getSignalChannelIndexes(chanSpec);
        data.putItem("SIGNAL_CHANNELS", signalChannels);

        final String referenceChannels = sampleHelper.getReferenceChannelIndexes(chanSpec);
        data.putItem("REFERENCE_CHANNEL", referenceChannels);

        boolean hasLamina = false;

        List<AnatomicalArea> sampleAreas = getSampleAreas(sampleEntity);

        if (sampleAreas != null) {
            data.putItem("SAMPLE_AREA", sampleAreas);
            hasLamina = hasLamina(sampleAreas);
        }

        data.putItem("SAMPLE_HAS_LAMINA", hasLamina);
    }

    private List<AnatomicalArea> getSampleAreas(Entity sampleEntity) throws Exception {

        List<AnatomicalArea> sampleAreas = null;

        populateChildren(sampleEntity);
        Entity supportingFiles = EntityUtils.getSupportingData(sampleEntity);

        if (supportingFiles == null) {

            contextLogger.info("skipping SAMPLE_AREA derviation, no supporting files for sample " +
                               sampleEntity.getName());

        } else {

            supportingFiles = entityBean.getEntityTree(supportingFiles.getId());
            final List<Entity> tileEntities = EntityUtils.getDescendantsOfType(supportingFiles,
                                                                               EntityConstants.TYPE_IMAGE_TILE,
                                                                               true);

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
        }

        return sampleAreas;
    }

    private boolean hasLamina(List<AnatomicalArea> sampleAreas) {
        boolean hasLamina = false;
        for (AnatomicalArea area : sampleAreas) {
            if (area.hasLamina()) {
                hasLamina = true;
                break;
            }
        }
        return hasLamina;
    }
}
