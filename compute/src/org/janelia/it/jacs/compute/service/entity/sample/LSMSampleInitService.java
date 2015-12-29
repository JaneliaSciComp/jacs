package org.janelia.it.jacs.compute.service.entity.sample;

import com.google.common.base.Joiner;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.service.entity.sample.SampleHelper;
import org.janelia.it.jacs.compute.service.entity.sample.SlideImage;
import org.janelia.it.jacs.compute.service.entity.sample.SlideImageGroup;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import java.util.*;

/**
 * Discovers the SAGE samples associated with the given LSM and processes the corresponding samples.
 */
public class LSMSampleInitService extends AbstractEntityService {

    private SampleHelper sampleHelper;

    public void execute() throws Exception {
        sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger);

        String owner = processData.getString("TASK_OWNER");
        List<String> lsmPaths = (List<String>) processData.getMandatoryItem("LSM_PATHS");

        SageDAO sageDao = new SageDAO(logger);

        Map<String, Multimap<String,SlideImage>> slideGroupsByDataset = new HashMap<>();
        for (String lsmPath : lsmPaths) {
            try {
                SlideImage slideImage = sageDao.getSlideImageByOwnerAndLSMPath(owner, lsmPath);
                String datasetName  = slideImage.getDatasetName();
                Multimap<String,SlideImage> slideGroups = slideGroupsByDataset.get(datasetName);
                if (slideGroups == null) {
                    slideGroups = LinkedListMultimap.create();
                    slideGroupsByDataset.put(datasetName, slideGroups);
                }
                slideGroups.put(slideImage.getSlideCode(), slideImage);
            } catch (DaoException e) {
                logger.warn("Error while retrieving image for " + lsmPath, e);
            }
        }

        List<String> sampleEntityIds = new ArrayList<>();
        for (String datasetName : slideGroupsByDataset.keySet()) {
            prepareSlideImageGroupsForDataset(owner, datasetName, slideGroupsByDataset.get(datasetName), sampleEntityIds);
        }

        processData.putItem("SAMPLE_ENTITY_ID", Joiner.on(',').join(sampleEntityIds));
    }

    private void prepareSlideImageGroupsForDataset(String owner, String datasetName,
                                                   Multimap<String, SlideImage> slideImagesGroupedBySlideCode,
                                                   List<String> sampleEntityIds) {
        List<Entity> datasets;
        try {
            datasets = entityBean.getEntitiesByNameAndTypeName(owner, datasetName, EntityConstants.TYPE_DATA_SET);
        } catch (Exception e) {
            logger.error("Error retrieving dataset entities for " + datasetName, e);
            return;
        }
        for (Entity dataset : datasets) {
            for (String slideCode : slideImagesGroupedBySlideCode.keys()) {
                try {
                    prepareSlideImageGroupsBySlideCode(dataset, slideCode,
                            slideImagesGroupedBySlideCode.get(slideCode),
                            sampleEntityIds);
                } catch (Exception e) {
                    logger.error("Error while preparing image groups for  " + datasetName + ": " + slideCode, e);
                }
            }
        }
    }

    private void prepareSlideImageGroupsBySlideCode(Entity dataset,
                                                    String slideCode,
                                                    Collection<SlideImage> slideImages,
                                                    List<String> sampleEntityIds) throws Exception {
        Map<String, SlideImageGroup> tileGroups = new LinkedHashMap<>();

        int tileNum = 0;
        for (SlideImage slideImage : slideImages) {
            String area = slideImage.getArea();
            String tag = slideImage.getTileType();
            if (tag==null) {
                tag = "Tile "+(tileNum+1);
            }

            String groupKey = area+"_"+tag;
            SlideImageGroup tileGroup = tileGroups.get(groupKey);
            if (tileGroup==null) {
                tileGroup = new SlideImageGroup(area, tag);
                tileGroups.put(groupKey, tileGroup);
            }
            tileGroup.addFile(slideImage);
            tileNum++;
        }
        List<SlideImageGroup> tileGroupList = new ArrayList<>(tileGroups.values());

        Entity sampleEntity = sampleHelper.createOrUpdateSample(null, slideCode, dataset, tileGroupList);
        sampleEntityIds.add(sampleEntity.getId().toString());
    }
}
